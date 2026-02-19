import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Modal } from '../components/Modal'
import { SkeletonTableRows } from '../components/Skeleton'
import { ApiError } from '../services/apiClient'
import { getCategories as fetchAllCategories } from '../services/contributionService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import type {
  ContributionCategoryResponse,
  ContributionCategoryRequest,
} from '../types/contributions'
import './Operations.css'

function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return fallback
}

export function ContributionCategories() {
  const { request } = useAuthenticatedApi()
  const { canAccess } = useAuthorization()
  const canManageCategories = canAccess(['ADMIN', 'TREASURER'])

  const [categories, setCategories] = useState<ContributionCategoryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [showModal, setShowModal] = useState(false)
  const [editingCategory, setEditingCategory] = useState<ContributionCategoryResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const loadCategories = useCallback(async () => {
    if (!canManageCategories) {
      setLoading(false)
      setCategories([])
      return
    }

    setLoading(true)
    try {
      const data = await fetchAllCategories(false, request)
      setCategories(data)
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to load categories.') })
    } finally {
      setLoading(false)
    }
  }, [canManageCategories, request])

  useEffect(() => {
    void loadCategories()
  }, [loadCategories])

  if (!canManageCategories) {
    return (
      <div className="ops-page">
        <div className="page-header">
          <div>
            <h1 className="page-title">Contribution Categories</h1>
            <p className="page-subtitle">Manage contribution categories</p>
          </div>
        </div>

        <hr className="rule rule--strong" />

        <div className="ops-feedback ops-feedback--error" role="status">
          Only admins and treasurers can manage contribution categories.
        </div>
      </div>
    )
  }

  function handleOpenAdd() {
    setEditingCategory(null)
    setShowModal(true)
  }

  function handleOpenEdit(category: ContributionCategoryResponse) {
    setEditingCategory(category)
    setShowModal(true)
  }

  function handleCloseModal() {
    setShowModal(false)
    setEditingCategory(null)
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const formData = new FormData(form)

    const name = (formData.get('name') as string).trim()
    const description = (formData.get('description') as string | null)?.trim() || undefined
    const isMandatory = formData.get('isMandatory') === 'on'

    if (!name) return

    setSubmitting(true)
    setFeedback(null)

    try {
      if (editingCategory) {
        const payload: ContributionCategoryRequest = {
          name,
          description,
          active: editingCategory.active,
          isMandatory,
        }
        const updated = await request<ContributionCategoryResponse>(
          `/api/v1/contribution-categories/${editingCategory.id}`,
          { method: 'PUT', body: JSON.stringify(payload) },
        )
        setCategories(prev => prev.map(c => c.id === updated.id ? updated : c))
        setFeedback({ type: 'success', text: `Category "${updated.name}" updated.` })
      } else {
        const payload: ContributionCategoryRequest = { name, description, isMandatory }
        const created = await request<ContributionCategoryResponse>(
          '/api/v1/contribution-categories',
          { method: 'POST', body: JSON.stringify(payload) },
        )
        setCategories(prev => [created, ...prev])
        setFeedback({ type: 'success', text: `Category "${created.name}" created.` })
      }
      handleCloseModal()
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to save category.') })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(category: ContributionCategoryResponse) {
    if (!window.confirm(`Delete category "${category.name}"? This action cannot be undone.`)) return

    setFeedback(null)
    try {
      await request<void>(`/api/v1/contribution-categories/${category.id}`, { method: 'DELETE' })
      setCategories(prev => prev.filter(c => c.id !== category.id))
      setFeedback({ type: 'success', text: `Category "${category.name}" deleted.` })
    } catch (error) {
      setFeedback({ type: 'error', text: toErrorMessage(error, 'Unable to delete category.') })
    }
  }

  return (
    <div className="ops-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Contribution Categories</h1>
          <p className="page-subtitle">Manage contribution categories</p>
        </div>
        <button type="button" className="btn btn--primary" onClick={handleOpenAdd}>
          Add Category
        </button>
      </div>

      <hr className="rule rule--strong" />

      {feedback && (
        <div className={`ops-feedback ops-feedback--${feedback.type}`} role="status">
          {feedback.text}
        </div>
      )}

      <table className="ledger-table">
        <thead>
          <tr>
            <th className="label">Category</th>
            <th className="label">Mandatory</th>
            <th className="label">Status</th>
            <th className="label">Actions</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <SkeletonTableRows cols={4} />
          ) : categories.length === 0 ? (
            <tr><td colSpan={4} className="table-empty">No categories found.</td></tr>
          ) : categories.map((category, i) => (
            <tr key={category.id} className={i % 2 === 1 ? 'ledger-row--alt' : ''}>
              <td>
                <span className="ops-member-name">{category.name}</span>
                <span className="ops-member-sub">{category.description}</span>
              </td>
              <td>
                <span className={`badge ${category.isMandatory ? 'badge--active' : 'badge--inactive'}`}>
                  {category.isMandatory ? 'Yes' : 'No'}
                </span>
              </td>
              <td>
                <span className={`badge ${category.active ? 'badge--active' : 'badge--inactive'}`}>
                  {category.active ? 'Active' : 'Inactive'}
                </span>
              </td>
              <td>
                <div className="ops-inline-actions">
                  <button
                    type="button"
                    className="btn btn--secondary btn--small"
                    onClick={() => handleOpenEdit(category)}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn--secondary btn--small"
                    onClick={() => void handleDelete(category)}
                  >
                    Delete
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <Modal
        open={showModal}
        onClose={handleCloseModal}
        title={editingCategory ? 'Edit Category' : 'Add Category'}
        width="sm"
        footer={
          <div className="ops-inline-actions">
            <button type="button" className="btn btn--secondary" onClick={handleCloseModal}>
              Cancel
            </button>
            <button type="submit" form="category-form" className="btn btn--primary" disabled={submitting}>
              {submitting ? 'Saving...' : editingCategory ? 'Update' : 'Create'}
            </button>
          </div>
        }
      >
        <form id="category-form" onSubmit={e => void handleSubmit(e)}>
          <div className="form-group">
            <label htmlFor="cat-name" className="form-label">Name</label>
            <input
              id="cat-name"
              name="name"
              type="text"
              className="form-input"
              required
              defaultValue={editingCategory?.name ?? ''}
              key={editingCategory?.id ?? 'new'}
            />
          </div>
          <div className="form-group">
            <label htmlFor="cat-description" className="form-label">Description</label>
            <textarea
              id="cat-description"
              name="description"
              className="form-input"
              rows={3}
              defaultValue={editingCategory?.description ?? ''}
              key={`desc-${editingCategory?.id ?? 'new'}`}
            />
          </div>
          <div className="form-group">
            <label className="form-checkbox-label">
              <input
                type="checkbox"
                name="isMandatory"
                defaultChecked={editingCategory?.isMandatory ?? false}
                key={`mandatory-${editingCategory?.id ?? 'new'}`}
              />
              Mandatory category
            </label>
          </div>
        </form>
      </Modal>
    </div>
  )
}

import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { DataTable, type ColumnDef } from '../components/DataTable'
import { ActionMenu } from '../components/ActionMenu'
import { Breadcrumb } from '../components/Breadcrumb'
import { Modal } from '../components/Modal'
import { ApiError } from '../services/apiClient'
import { getCategories as fetchAllCategories } from '../services/contributionService'
import { useAuthenticatedApi } from '../hooks/useAuthenticatedApi'
import { useAuthorization } from '../hooks/useAuthorization'
import { useToast } from '../hooks/useToast'
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
  const toast = useToast()
  const canManageCategories = canAccess(['ADMIN', 'TREASURER'])

  const [categories, setCategories] = useState<ContributionCategoryResponse[]>([])
  const [loading, setLoading] = useState(true)
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
      toast.error('Unable to load categories', toErrorMessage(error, 'Unable to load categories.'))
    } finally {
      setLoading(false)
    }
  }, [canManageCategories, request, toast])

  useEffect(() => {
    void loadCategories()
  }, [loadCategories])

  if (!canManageCategories) {
    return (
      <div className="ops-page">
        <div className="page-header">
          <div>
            <h1 className="page-title">Contribution Categories</h1>
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
    const mandatory = formData.get('isMandatory') === 'on'
    const welfareEligible = formData.get('welfareEligible') === 'on'

    if (!name) return

    setSubmitting(true)

    try {
      if (editingCategory) {
        const payload: ContributionCategoryRequest = {
          name,
          description,
          active: editingCategory.active,
          mandatory,
          welfareEligible,
        }
        const updated = await request<ContributionCategoryResponse>(
          `/api/v1/contribution-categories/${editingCategory.id}`,
          { method: 'PUT', body: JSON.stringify(payload) },
        )
        setCategories(prev => prev.map(c => c.id === updated.id ? updated : c))
        toast.success('Category updated', `"${updated.name}" updated.`)
      } else {
        const payload: ContributionCategoryRequest = { name, description, mandatory, welfareEligible }
        const created = await request<ContributionCategoryResponse>(
          '/api/v1/contribution-categories',
          { method: 'POST', body: JSON.stringify(payload) },
        )
        setCategories(prev => [created, ...prev])
        toast.success('Category created', `"${created.name}" created.`)
      }
      handleCloseModal()
    } catch (error) {
      toast.error('Unable to save category', toErrorMessage(error, 'Unable to save category.'))
    } finally {
      setSubmitting(false)
    }
  }

  const categoryColumns: ColumnDef<ContributionCategoryResponse>[] = [
    {
      key: 'category',
      header: 'Category',
      render: category => (
        <>
          <span className="ops-member-name">{category.name}</span>
          <span className="ops-member-sub">{category.description}</span>
        </>
      ),
    },
    {
      key: 'mandatory',
      header: 'Mandatory',
      render: category => (
        <span className={`badge ${(category.isMandatory ?? category.mandatory ?? false) ? 'badge--active' : 'badge--inactive'}`}>
          {(category.isMandatory ?? category.mandatory ?? false) ? 'Yes' : 'No'}
        </span>
      ),
    },
    {
      key: 'welfare',
      header: 'Welfare Eligible',
      render: category => (
        <span className={`badge ${category.welfareEligible ? 'badge--active' : 'badge--inactive'}`}>
          {category.welfareEligible ? 'Yes' : 'No'}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: category => (
        <span className={`badge ${category.active ? 'badge--active' : 'badge--inactive'}`}>
          {category.active ? 'Active' : 'Inactive'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: '',
      width: '52px',
      headerClassName: 'datatable-col-actions',
      className: 'datatable-col-actions',
      render: category => (
        <ActionMenu
          actions={[
            { label: 'Edit', onClick: () => handleOpenEdit(category) },
            { label: 'Delete', onClick: () => void handleDelete(category), variant: 'danger' },
          ]}
        />
      ),
    },
  ]

  async function handleDelete(category: ContributionCategoryResponse) {
    if (!window.confirm(`Delete category "${category.name}"? This action cannot be undone.`)) return

    try {
      await request<void>(`/api/v1/contribution-categories/${category.id}`, { method: 'DELETE' })
      setCategories(prev => prev.filter(c => c.id !== category.id))
      toast.success('Category deleted', `"${category.name}" deleted.`)
    } catch (error) {
      toast.error('Unable to delete category', toErrorMessage(error, 'Unable to delete category.'))
    }
  }

  return (
    <div className="ops-page">
      <Breadcrumb items={[
        { label: 'Operations', to: '/operations' },
        { label: 'Contribution Categories' },
      ]} />
      <div className="page-header">
        <div>
          <h1 className="page-title">Contribution Categories</h1>
        </div>
        <button type="button" className="btn btn--primary" onClick={handleOpenAdd}>
          Add Category
        </button>
      </div>

      <hr className="rule rule--strong" />

      <DataTable<ContributionCategoryResponse>
        columns={categoryColumns}
        data={categories}
        getRowKey={row => row.id}
        loading={loading}
        emptyMessage="No categories found."
        getRowClassName={(_, i) => i % 2 === 1 ? 'datatable-row--alt' : ''}
      />

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
        <form id="category-form" className="auth-form" onSubmit={e => void handleSubmit(e)}>
          <div className="field">
            <label htmlFor="cat-name" className="field-label">Name</label>
            <input
              id="cat-name"
              name="name"
              type="text"
              className="field-input"
              required
              defaultValue={editingCategory?.name ?? ''}
              key={editingCategory?.id ?? 'new'}
            />
          </div>
          <div className="field">
            <label htmlFor="cat-description" className="field-label">Description</label>
            <textarea
              id="cat-description"
              name="description"
              className="field-input"
              rows={3}
              defaultValue={editingCategory?.description ?? ''}
              key={`desc-${editingCategory?.id ?? 'new'}`}
            />
          </div>
          <div className="field">
            <label className="field-checkbox">
              <input
                type="checkbox"
                name="isMandatory"
                defaultChecked={editingCategory?.isMandatory ?? editingCategory?.mandatory ?? false}
                key={`mandatory-${editingCategory?.id ?? 'new'}`}
              />
              <span>Mandatory category</span>
            </label>
          </div>
          <div className="field">
            <label className="field-checkbox">
              <input
                type="checkbox"
                name="welfareEligible"
                defaultChecked={editingCategory?.welfareEligible ?? false}
                key={`welfare-${editingCategory?.id ?? 'new'}`}
              />
              <span>Welfare split eligible</span>
            </label>
          </div>
        </form>
      </Modal>
    </div>
  )
}

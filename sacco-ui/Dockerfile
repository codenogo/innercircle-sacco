FROM node:22-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG APP_COMMIT_SHA=unknown
ARG APP_COMMIT_SHORT_SHA=unknown
ARG APP_BUILD_DATE=unknown

RUN mkdir -p public \
    && printf '{\n  "commit": "%s",\n  "shortCommit": "%s",\n  "builtAt": "%s"\n}\n' "$APP_COMMIT_SHA" "$APP_COMMIT_SHORT_SHA" "$APP_BUILD_DATE" > public/build-info.json \
    && npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist/ /usr/share/nginx/html/

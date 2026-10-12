FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine
# Defaults for docker compose (Docker's embedded DNS + compose service names).
# Kubernetes overrides these with kube-dns and fully-qualified service names.
ENV NGINX_RESOLVER=127.0.0.11 \
    PATIENT_UPSTREAM=http://patient-service:8080 \
    ANALYTICS_UPSTREAM=http://analytics-service:8081 \
    NOTIFICATION_UPSTREAM=http://notification-service:8082 \
    ASSISTANT_UPSTREAM=http://ai-assistant-service:8083
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080

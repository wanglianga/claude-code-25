# ---------- 阶段 1：构建前端 ----------
FROM node:20-alpine AS frontend
WORKDIR /fe
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---------- 阶段 2：构建后端（前端产物并入静态资源） ----------
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /be
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /fe/dist ./src/main/resources/static
RUN mvn -q -B package -DskipTests

# ---------- 阶段 3：运行时（非 root 用户） ----------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=backend /be/target/nursery-platform.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/api/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

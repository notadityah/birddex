# Stage 1 — compile TypeScript and bundle dependencies
FROM node:22-alpine AS build

WORKDIR /build
RUN npm install -g esbuild

# Install all backend deps so esbuild can resolve and bundle imports.
# --ignore-scripts prevents onnxruntime-node from downloading its native binary
# in the build stage (the final image handles that).
COPY package.json ./
RUN npm install --ignore-scripts

COPY lambda/tsconfig.json ./tsconfig.json
COPY lambda/lib/ ./lib/
COPY lambda/detect/index.ts ./detect/index.ts

# Bundle everything except onnxruntime-node (native binary, installed in stage 2)
RUN esbuild detect/index.ts \
  --bundle \
  --platform=node \
  --target=node22 \
  --external:onnxruntime-node \
  --external:sharp \
  --outfile=index.js \
  --minify

# Stage 2 — Lambda runtime with native binary
FROM public.ecr.aws/lambda/nodejs:22

WORKDIR ${LAMBDA_TASK_ROOT}

COPY lambda/detect/package.json ./
RUN npm install --omit=dev

COPY --from=build /build/index.js ./

CMD ["index.handler"]

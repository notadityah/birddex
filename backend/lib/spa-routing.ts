/**
 * CloudFront Function (viewer-request) that does SPA routing for the S3 default behavior.
 *
 * Any request whose last path segment has no file extension (`/`, `/mydex`, `/gallery/`)
 * is rewritten to `/index.html`, so Vue Router can take over on a hard refresh. Requests
 * for real files (`/assets/index-abc123.js`, `/favicon.ico`) pass through unchanged, so a
 * missing asset gets S3's real 404 instead of a 200 index.html.
 *
 * Why not distribution-level `errorResponses` (403/404 -> 200 /index.html)? CloudFront
 * applies custom error responses to every cache behavior, `/api/*` included, and they
 * can't be scoped to one behavior. They turned API 403/404 responses into
 * `200 text/html` (e.g. better-auth's 403 on sign-out or unverified sign-in). A function
 * only runs on the behavior it is attached to.
 *
 * The source targets the `cloudfront-js-2.0` runtime. It is exported as a string so the
 * stack can inline it and the tests can run the exact code that gets deployed.
 */
export const SPA_REWRITE_FUNCTION_CODE = `function handler(event) {
  var request = event.request;
  var uri = request.uri;
  var lastSegment = uri.substring(uri.lastIndexOf('/') + 1);
  if (lastSegment.indexOf('.') === -1) {
    request.uri = '/index.html';
  }
  return request;
}
`;

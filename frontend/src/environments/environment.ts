// Development defaults. Requests go to '/api', which the Angular dev server
// proxies to the Spring Boot backend on :8080 (see proxy.conf.json). Using
// the proxy avoids CORS configuration on the backend during development.
export const environment = {
  production: false,
  apiUrl: '/api',
};

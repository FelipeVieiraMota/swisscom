# Links API

Manages short links and performs public redirects.

## Endpoints

- `POST /api/v1/links` creates a link for the authenticated user.
- `GET /api/v1/links` lists the authenticated user's links.
- `GET /api/v1/links/{id}` returns one owned link.
- `DELETE /api/v1/links/{id}` deactivates one owned link.
- `GET /r/{shortCode}` redirects without authentication.

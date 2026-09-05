# Dial Health Screen Factory APK — Persistent Google Drive Authorization

The APK opens the stable Screen Factory entry point and preserves the existing access-key gate. Once the live Oracle dashboard loads it checks the Google Drive archive credential.

If Drive is not authorized, the dashboard is covered by a boot-time Google authorization gate. The user signs in once as `tapiwaguduza@gmail.com`; Google returns to the stable GitHub Pages callback, which deep-links to `dialhealthscreenfactory://oauth/google`. Android forwards the authorization result back through the stable entry point to Oracle for server-side exchange and secure storage.

The APK contains no Google client secret or refresh token. Oracle stores the refresh credential and rclone refreshes short-lived Google access tokens automatically. The gate remains hidden while that persistent authorization is healthy and returns if access is revoked or invalidated.

Google Auth Platform must use a Web application OAuth client whose authorized redirect URI is:

`https://vanguduza.github.io/dial-health-screen-factory-dashboard/oauth/google/callback.html`

For long-lived unattended access, move the OAuth app from Testing to In production before final authorization; Testing refresh tokens expire after seven days.

# Checklist de migracao (Angular para React)

Uma linha por it(...) dos specs do Angular. Tasks 2-7 marcam cada item; sai na Task 8.

## src/app/app.routes.spec.ts

- [ ] L17: 'redirects the old /avisos path to /notificacoes', async () => {
- [ ] L91: `shows the error screen on ${url} with ${endpoint} in 503, without a loop`, async () => {
- [ ] L108: 'opens the page that was asked when the retry works', async () => {
- [ ] L120: 'shows the error screen after a right sign-in with the profile failing', async () => {
- [ ] L135: 'retries from the start when the error address itself was opened', async () => {

## src/app/app.spec.ts

- [ ] L6: 'hosts the router outlet', async () => {

## src/app/core/auth.interceptor.spec.ts

- [x] L35: 'sends the CSRF header on non-GET requests', () => { -> src/lib/bff-client.test.ts: adds X-CSRF-Token from bank_csrf on POST
- [x] L42: 'does not send the CSRF header on GET', () => { -> src/lib/bff-client.test.ts: does not add it on GET
- [x] L49: 'does not send the CSRF header to other origins', () => { -> src/lib/bff-client.test.ts: does not add it outside /bff/
- [x] L56: 'clears the session and goes to Entrar on 401', () => { -> src/lib/bff-client.test.ts + session.test.ts: 401 outside /bff/auth notifies session lost once; 401 outside /bff/auth clears the session (redirect to /entrar is the guard in Task 3)
- [x] L66: 'lets a wrong password on sign in reach the screen', () => { -> src/lib/bff-client.test.ts: 401 on /bff/auth/sign-in does not

## src/app/core/guards.spec.ts

- [ ] L38: `${name} sends a 503 of ${failing} to the error screen`, async () => {
- [ ] L58: 'authGuard sends an anonymous visitor to Entrar', async () => {
- [ ] L64: 'authGuard lets an authenticated visitor through', async () => {
- [ ] L72: 'profileGuard sends a user without customer to Completar cadastro', async () => {
- [ ] L80: 'profileGuard lets a registered customer through', async () => {
- [ ] L91: 'unregisteredGuard sends a registered customer to Início', async () => {
- [ ] L102: 'unregisteredGuard lets a user without customer through', async () => {
- [ ] L110: 'unregisteredGuard sends an anonymous visitor to Entrar', async () => {
- [ ] L116: 'profileGuard sends an anonymous visitor to Entrar', async () => {
- [ ] L122: 'guestGuard sends an authenticated visitor into the app', async () => {
- [ ] L130: 'guestGuard lets an anonymous visitor see Entrar', async () => {

## src/app/core/session.service.spec.ts

- [x] L23: 'loads an anonymous session', async () => { -> src/lib/session.test.ts: loads an anonymous session
- [x] L31: 'loads the profile when authenticated', async () => { -> src/lib/session.test.ts: loads the profile when authenticated
- [x] L43: 'treats a failed session call as anonymous', async () => { -> src/lib/session.test.ts: treats a failed session call as anonymous
- [x] L50: 'keeps the session when the BFF is unavailable (503)', async () => { -> src/lib/session.test.ts: keeps the session when the BFF is unavailable (503)
- [x] L63: 'clears the session when the BFF answers 401', async () => { -> src/lib/session.test.ts: clears the session when the BFF answers 401
- [x] L72: 'does not reload a loaded session', async () => { -> src/lib/session.test.ts: does not reload a loaded session
- [x] L80: 'signs in and then loads the session', async () => { -> src/lib/session.test.ts: signs in and then loads the session
- [x] L97: 'signs up and reports whether a code is required', async () => { -> src/lib/session.test.ts: signs up and reports whether a code is required
- [x] L103: 'confirms the sign up code', async () => { -> src/lib/session.test.ts: confirms the sign up code
- [x] L111: 'signs out, clears the state and goes to Entrar', async () => { -> src/lib/session.test.ts: signs out and clears the state (redirect to /entrar is Task 3)
- [x] L127: `does not declare the sign-out done when the POST fails with ${status}`, async () => { -> src/lib/session.test.ts: does not declare the sign-out done when the POST fails with %d
- [x] L143: 'counts a session that no longer exists (401) as signed out', async () => { -> src/lib/session.test.ts: counts a session that no longer exists (401) as signed out
- [x] L154: 'forgets a failed load when the session is cleared', () => { -> src/lib/session.test.ts: forgets a failed load when the session is cleared
- [x] L160: 'forgets a failed sign-out when the session is cleared', () => { -> src/lib/session.test.ts: forgets a failed sign-out when the session is cleared
- [x] L166: 'loads the unread count', async () => { -> src/lib/session.test.ts: loads the unread count
- [x] L173: 'keeps the last unread count when the call fails', async () => { -> src/lib/session.test.ts: keeps the last unread count when the call fails
- [x] L181: 'does not overlap unread calls', async () => { -> src/lib/session.test.ts: does not overlap unread calls
- [x] L189: 'does not let a count requested before marking as read overwrite the new one', async () => { -> src/lib/session.test.ts: does not let a count requested before marking as read overwrite the new one
- [x] L201: 'keeps one call in flight after a fresh read finishes behind a stale one', async () => { -> src/lib/session.test.ts: keeps one call in flight after a fresh read finishes behind a stale one
- [x] L214: 'ignores a count that arrives after clear and does not reuse that call for the next user', async () => { -> src/lib/session.test.ts: stale unread answer is discarded after clear
- [x] L248: 'refreshes every 30 seconds while the tab is visible', async () => { -> src/lib/session.test.ts: refreshes every 30 seconds while the tab is visible
- [x] L260: 'skips the tick while the tab is hidden', async () => { -> src/lib/session.test.ts: skips the tick while the tab is hidden
- [x] L267: 'refreshes right away when the tab becomes visible again', async () => { -> src/lib/session.test.ts: refreshes right away when the tab becomes visible again
- [x] L277: 'does not start a second timer when started twice', async () => { -> src/lib/session.test.ts: does not start a second timer when started twice
- [x] L284: 'stops when asked and when the session is cleared', async () => { -> src/lib/session.test.ts: stops when asked and when the session is cleared

## src/app/pages/account/account.component.spec.ts

- [ ] L53: 'signs out from Minha conta', async () => {
- [ ] L65: 'shows the customer and the account data', async () => {
- [ ] L74: 'changes the phone sending only digits and reloads the profile', async () => {
- [ ] L91: 'blocks a phone that is not a mobile number', async () => {
- [ ] L99: 'shows the message when the phone is already used', async () => {
- [ ] L111: 'asks for confirmation before closing the account', async () => {
- [ ] L123: 'closes the account after the confirmation and reloads the profile', async () => {
- [ ] L139: 'shows why the account could not be closed', async () => {
- [ ] L154: 'has no close button when there is no account', async () => {

## src/app/pages/complete-profile/complete-profile.component.spec.ts

- [ ] L33: 'masks the phone while typing', async () => {
- [ ] L39: 'blocks the submit without a valid mobile phone', async () => {
- [ ] L48: 'blocks the submit without a name', async () => {
- [ ] L54: 'registers the customer and goes to the app', async () => {
- [ ] L74: 'leaves the registration when it worked and only the profile load failed', async () => {
- [ ] L90: 'shows the BFF message when the customer already exists', async () => {
- [ ] L107: 'shows the problem detail when the phone is already used', async () => {

## src/app/pages/home/home.component.spec.ts

- [ ] L57: 'shows the balance card, the daily limit and the latest entries', async () => {
- [ ] L78: 'fills the limit bar in proportion to what was used', async () => {
- [ ] L89: 'offers "Abrir conta" without calling /bff/home when the session has no account, and opens it', async () => {
- [ ] L118: 'falls back to "Abrir conta" when /bff/home answers 404', async () => {
- [ ] L130: 'shows the message of a failed account opening', async () => {
- [ ] L147: 'shows a discreet notice with a retry when the load fails', async () => {

## src/app/pages/notifications/notifications.component.spec.ts

- [ ] L44: 'lists the notifications with the unread ones in bold', async () => {
- [ ] L60: 'marks everything as read up to the newest notification and refreshes the counter', async () => {
- [ ] L89: 'does not let a count requested before marking as read bring the number back', async () => {
- [ ] L116: 'says so when there are no notifications', async () => {
- [ ] L123: 'shows the retry notice when the load fails', async () => {

## src/app/pages/sign-in/sign-in.component.spec.ts

- [ ] L29: 'shows Entrar as the page heading with a link to Criar conta', () => {
- [ ] L35: ['12345', '1234567', '12a456', ''])(
- [ ] L47: 'signs in with six digits and goes to the app', async () => {
- [ ] L64: 'shows the error when the credentials are wrong', async () => {

## src/app/pages/sign-up/sign-up.component.spec.ts

- [ ] L46: [
- [ ] L57: 'blocks the submit when the confirmation differs', async () => {
- [ ] L65: 'asks for the e-mail code only when the BFF requires it', async () => {
- [ ] L87: 'skips the code and signs in when no confirmation is required', async () => {
- [ ] L96: 'shows the BFF message when the e-mail is already taken', async () => {
- [ ] L111: 'accepts only a 6 digit e-mail code', async () => {
- [ ] L127: 'shows the problem detail when the e-mail is already taken', async () => {

## src/app/pages/statement/statement.component.spec.ts

- [ ] L43: 'shows "Bônus de boas-vindas" as an entry, with the down arrow', async () => {
- [ ] L55: 'loads the next page by the cursor and hides the button on the last one', async () => {
- [ ] L72: 'says so when there are no entries', async () => {
- [ ] L78: 'shows the retry notice when the load fails and tries again', async () => {

## src/app/pages/transfer/transfer-attempt.store.spec.ts

- [x] L13: 'says whether the key already existed', async () => { -> src/lib/transfer-attempt.test.ts: says whether the key already existed
- [x] L21: 'a late settle of an older attempt does not drop the key of a newer one', async () => { -> src/lib/transfer-attempt.test.ts: a late settle of an older attempt does not drop the key of a newer one

## src/app/pages/transfer/transfer.component.spec.ts

- [ ] L115: 'starts at the phone step and blocks an invalid phone', async () => {
- [ ] L124: 'shows the full name of the recipient to be checked, then asks the amount', async () => {
- [ ] L132: 'confirms with the name and the amount before sending', async () => {
- [ ] L140: 'refuses an amount that is not a positive value', async () => {
- [ ] L148: 'shows the BFF message when the phone has no account to receive', async () => {
- [ ] L166: 'sends the amount in cents with the recipient token and an Idempotency-Key', async () => {
- [ ] L176: 'sends one POST on a double click on Confirmar', async () => {
- [ ] L189: 'keeps the same Idempotency-Key when the user retries after a failure', async () => {
- [ ] L203: 'uses a new Idempotency-Key when the amount changes after going back', async () => {
- [ ] L218: 'keeps the Idempotency-Key when going back and confirming the same amount', async () => {
- [ ] L232: 'fetches the reason right away when the POST already answers a final status', async () => {
- [ ] L247: 'keeps the key when the recipient is looked up again and gets another token', async () => {
- [ ] L258: 'keeps the key when the recipient token expired after a lost answer', async () => {
- [ ] L275: 'uses a new key for another recipient', async () => {
- [ ] L286: 'finds the first key again when the user comes back to the first amount', async () => {
- [ ] L301: 'keeps the key when the user leaves the page after a lost answer and returns', async () => {
- [ ] L311: 'keeps the key when the tab reloads after a lost answer', async () => {
- [ ] L321: 'keeps the key when the same user signs in again', async () => {
- [x] L334: 'never gives the key to another user of the tab, and keeps it for its owner', async () => { -> src/lib/transfer-attempt.test.ts: never gives the key to another user of the tab, and keeps it for its owner (store rule; the screen still goes to Task 7)
- [x] L352: 'does not leave the e-mail or the phone readable in the tab storage', async () => { -> src/lib/transfer-attempt.test.ts: does not leave the e-mail or the phone readable in the tab storage
- [ ] L361: 'keeps the key when the user leaves before the final status', async () => {
- [ ] L373: 'keeps the key when the final answer arrives after the user left the page', async () => {
- [ ] L387: 'keeps the key for "Nova transferência" while the status is still not final', async () => {
- [ ] L404: `uses a new key for the same transfer after ${status} in the POST answer`, async () => {
- [ ] L419: `uses a new key after leaving a transfer that polling showed ${status}`, async () => {
- [ ] L441: 'warns when "Nova transferência" after "Ainda processando" sends the same transfer', async () => {
- [ ] L462: 'warns when the user left with the POST in flight and sends the same transfer later', async () => {
- [ ] L478: 'does not warn on the first transfer', async () => {
- [ ] L488: 'does not warn on an equal transfer after a final status was seen', async () => {
- [ ] L508: 'cannot go back until the POST ends, then can', async () => {
- [ ] L526: 'says the send could not be confirmed and that trying here is safe', async () => {
- [ ] L540: 'sends nothing when reading the storage throws', async () => {
- [ ] L551: 'sends nothing when writing the storage throws', async () => {
- [ ] L562: 'sends nothing when crypto.subtle is missing', async () => {
- [ ] L585: 'polls every second and shows the transfer as completed, then stops', async () => {
- [ ] L596: 'shows the reason of a refused transfer and stops polling', async () => {
- [ ] L605: 'shows the reason of a returned (compensated) transfer', async () => {
- [ ] L614: 'keeps polling after a failed poll', async () => {
- [ ] L623: 'gives up after 30 s with "Ainda processando"', async () => {
- [ ] L637: 'starts a new transfer with a new Idempotency-Key', async () => {

## src/app/shared/amount.spec.ts

- [x] L4: [ -> src/lib/money.test.ts: reads %s as %d cents
- [x] L16: [ -> src/lib/money.test.ts: refuses %s
- [x] L32: 'never goes through floating point', () => { -> src/lib/money.test.ts: never goes through floating point

## src/app/shared/app-footer.component.spec.ts

- [ ] L5: 'shows the copyright line', async () => {

## src/app/shared/entry-icon.component.spec.ts

- [ ] L12: 'shows a filled circle with a down arrow for a credit', async () => {
- [ ] L20: 'shows a hollow circle with an up arrow for a debit', async () => {

## src/app/shared/entry-list.component.spec.ts

- [ ] L30: 'shows the welcome bonus as an entry (down arrow, bold amount)', async () => {
- [ ] L40: 'shows a debit as an exit (up arrow, grey amount)', async () => {
- [ ] L49: 'falls back to the entry kind when the description is null', async () => {

## src/app/shared/money.pipe.spec.ts

- [x] L6: 'formats cents as reais with thousands separator', () => { -> src/lib/money.test.ts: formats cents as reais with thousands separator
- [x] L10: 'puts the sign before the currency symbol', () => { -> src/lib/money.test.ts: puts the sign before the currency symbol
- [x] L14: 'pads cents below one real', () => { -> src/lib/money.test.ts: pads cents below one real
- [x] L19: 'groups millions', () => { -> src/lib/money.test.ts: groups millions
- [x] L23: 'shows a dash for missing or fractional values', () => { -> src/lib/money.test.ts: shows a dash for missing or fractional values

## src/app/shared/password.spec.ts

- [x] L7: 'accepts exactly six digits', () => { -> src/lib/password.test.ts: accepts exactly six digits
- [x] L11: 'rejects five digits', () => { -> src/lib/password.test.ts: rejects five digits
- [x] L15: 'rejects seven digits', () => { -> src/lib/password.test.ts: rejects seven digits
- [x] L19: 'rejects letters', () => { -> src/lib/password.test.ts: rejects letters
- [x] L23: 'rejects empty', () => { -> src/lib/password.test.ts: rejects empty
- [x] L35: 'passes when equal', () => { -> src/lib/password.test.ts: passes when equal
- [x] L39: 'fails when different', () => { -> src/lib/password.test.ts: fails when different

## src/app/shared/phone.spec.ts

- [x] L5: 'masks progressively', () => { -> src/lib/phone.test.ts: masks progressively
- [x] L13: 'ignores non digits and extra digits', () => { -> src/lib/phone.test.ts: ignores non digits and extra digits
- [x] L18: 'validates a Brazilian mobile number', () => { -> src/lib/phone.test.ts: validates a Brazilian mobile number

## src/app/shell/app-shell.component.spec.ts

- [ ] L53: 'shows the footer', async () => {
- [ ] L58: 'shows a discreet notice with a retry button only when the session load failed', async () => {
- [ ] L70: 'lists the five menu items', async () => {
- [ ] L79: 'shows the unread counter next to Notificações', async () => {
- [ ] L84: 'shows one bell: at the top of the content when wide, in the top bar when narrow', async () => {
- [ ] L94: 'polls the unread counter while mounted and stops when destroyed', async () => {
- [ ] L102: 'starts the unread polling on init', async () => {
- [ ] L108: 'uses the 960 px breakpoint', async () => {
- [ ] L113: 'keeps the menu fixed and hides the hamburger at 960 px or wider', async () => {
- [ ] L119: 'collapses the menu below 960 px and opens it with the hamburger', async () => {
- [ ] L132: 'moves the focus to the first item when the menu opens and back to the hamburger on Escape', async () => {
- [ ] L149: 'reads the unread counter to screen readers as text', async () => {
- [ ] L156: 'closes the collapsed menu after choosing an item', async () => {
- [ ] L165: 'shows Sair as a visible menu item with an icon, below 960 px too', async () => {
- [ ] L176: 'signs out from the menu: calls the BFF, clears the session and goes to Entrar', async () => {
- [ ] L190: 'says that the sign-out failed and leaves only after a retry works', async () => {

## src/app/shell/notification-bell.component.spec.ts

- [ ] L58: 'shows the unread counter on the bell, as text for screen readers too', async () => {
- [ ] L67: 'opens a panel with the notifications and marks them read up to the newest', async () => {
- [ ] L87: 'does not let a count requested before marking as read bring the number back', async () => {
- [ ] L101: 'does not mark anything when everything is already read', async () => {
- [ ] L107: 'says so when there are no notifications', async () => {
- [ ] L114: 'fetches the next page by the cursor when the list is scrolled to the end', async () => {
- [ ] L131: 'does not fetch more before the end of the list', async () => {
- [ ] L142: 'shows the retry notice when the load fails and tries again', async () => {
- [ ] L154: 'closes with Escape and returns the focus to the bell', async () => {
- [ ] L165: 'closes on a click outside, but not on a click inside', async () => {
- [ ] L178: 'closes when the bell is clicked again', async () => {
- [ ] L186: 'closes on navigation', async () => {
- [ ] L194: 'reloads from the first page every time it opens', async () => {

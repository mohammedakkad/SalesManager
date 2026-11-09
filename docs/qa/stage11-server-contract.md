# Stage 11 — Firebase protocol and deployment boundary

The implementation uses Firebase SDK transactions, not independent blind uploads, for outbox-managed sale effects. No production Firebase requests, rules changes or deployments were performed during this task. SDK callback tests are deterministic fakes; Firebase Emulator Suite and deployed-rule acceptance are NOT TESTED.

## Projection compatibility

Within `merchants/{merchantId}`:

| Backend | Existing projections | Added metadata |
| --- | --- | --- |
| Firestore | `products/{productId}/units/{unitId}` quantity; `stock_movements/{movementId}`; `invoice_items/{itemId}` | `_sale_events/{eventId}`, `_sale_heads/{aggregateId}` |
| Realtime Database | `transactions/{id}`, `invoice_items/{id}`, `cash_boxes/{id}`, `cash_box_movements/{id}`, `return_invoices/{id}` | `_sale_events/{eventId}`, `_sale_heads/{aggregateId}` |

Existing projection field names are retained. Upserts merge known fields; explicit deletion removes the entity while leaving its event receipt/head. Stock quantities and cash balances are absolute values with a verified before-image; they are not unconditionally replaced or incremented repeatedly on retries. Movement IDs and event IDs remain stable.

Each backend transaction reads its event receipt, aggregate revision and all changed resources before writing. An absent receipt requires `remoteRevision == eventRevision - 1` and every resource to match its immutable before-image. A matching existing receipt is reusable only at the exact expected remote revision. A remote-ahead receipt is a conflict, preventing an older restored local state being labeled synchronized. The transaction writes effects, head and receipt together. Native transaction retries must re-evaluate these conditions; no timestamp winner is selected.

## Two-backend completion

Firestore runs first; RTDB runs second. Full acknowledgment requires success in BOTH phases. If execution stops between them, the durable Firestore receipt allows a retry to skip the completed phase. If RTDB conflicts after Firestore committed, the event is retained as CONFLICTING with an `AFTER_FIRESTORE` reason. This is partial remote completion requiring reconciliation; there is no automatic compensating refund, stock adjustment, conflict erasure or claim of cross-database ACID.

A successful event proves those conditional phases completed under the cooperating protocol. It does not prove that a legacy or privileged writer cannot subsequently overwrite a resource. Exactly-once Firebase processing across all clients is NOT guaranteed.

## Required server/deployment work

- Authorize merchant membership using verified server identity. A locally stored merchant code is not itself an authorization proof.
- Protect receipts as immutable and enforce revision/effect consistency. Fence all legacy direct financial writes, restores and older clients that do not advance protocol metadata.
- Provide a trusted reconciliation/processing contract for conflicts and partial cross-backend completion, with per-resource fencing for shared stock/cash and server-enforced deduplication. Treat each financial effect independently of a successful unrelated document write.
- Verify RTDB merchant-root transaction permissions/size and Firestore transaction limits under the actual rules. Current payload validation permits at most450 Firestore resource changes plus metadata,2000 total changes and512KiB serialized payload. Oversized/unsupported events become visible conflicts; they are not silently split into a claimed atomic batch.
- Establish remote catalog/cash baseline provisioning before accepting before-images. A missing or different remote baseline is a conflict, not permission to invent an opening balance or quantity.
- Define a versioned pull/reconciliation protocol. Managed sale/return/invoice rows ignore unversioned echoes; an unversioned deletion is quarantined rather than guessing stock/cash reversals.
- Retain receipts/heads for the supported replay/backup horizon. New local numeric sale IDs are not globally unique across devices; same-ID creations conflict and need an identity/migration strategy rather than overwrite.

## Session, restore and legacy limits

The dispatcher uses the current merchant, captures the target in the event, cancels work on merchant change and checks authorization again before acknowledgment. Cancellation cannot recall a request already accepted by Firebase; such completion remains uncertain locally and is retried using the same event identity. It is never redirected to the next account.

Restored worker ownership is invalid. Restore cannot remove/rewrite known outbox history, cannot clear a permanent conflict, and revalidates the latest acknowledged revision per aggregate. Historical acknowledgments retain their audit meaning; unresolved chains can require reconciliation if the server is ahead.

Shared legacy tables/APIs outside sale commands and managed returns do not become universally tenant-partitioned or outbox-backed in this increment. Product/manual-cash/other legacy writers need adoption/fencing, and legacy cache ownership on account switching needs a separate tenant-storage migration. Client-side pending guards reduce bypasses but cannot replace deployed server rules or cancel old accepted requests. These remain release blockers/limitations, not verified distributed guarantees.

## Authoritative rule source still required

Repository inspection found no Firebase rules/functions deployment source. User-provided console links point to production data views, not rule definitions; those data pages were not opened. Required input is rule text from each database’s Rules tab plus the actual identity/membership or Custom Claims contract. Do not infer authorization from locally saved activation codes or invent/deploy permissive rules. Seller import fencing is a local integrity improvement and does not establish server enforcement.

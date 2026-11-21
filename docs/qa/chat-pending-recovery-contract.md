# Seller chat pending-state and recovery contract

Qualified source: `d53b248b6091ca16d6df2a0c06debfac41af86ef`. This concerns `pending_messages` and ChatViewModel. It does not change the financial SaleCommands/Outbox protocol documented in stage10/stage11, Room schema 25, Firebase projections or server rules.

|Persisted row|Current ViewModel sendingIds|UI/actions|
|---|---|---|
|Present, either isFailed value|Contains tempId|Sending; retry/dismiss controls hidden; stale retry/dismiss callbacks rejected|
|Present, isFailed=true|Does not contain tempId|Failed; explicit retry or local dismissal|
|Present, isFailed=false|Does not contain tempId|Unconfirmed; explicit retry with the original ID, or local dismissal|
|Absent after successful send/delete|Irrelevant|No pending bubble; a server echo may be shown separately|

A new message is inserted before its remote send. Send/retry reserve their ID before coroutine dispatch and release it in finally. Ordinary remote failures retain a failed row. Cancellation propagates and does not invent a durable transport failure; a remaining committed row becomes unconfirmed when no attempt is active. A new ViewModel starts with no active IDs, so persisted pending rows remain recoverable without an automatic replay worker. Tests use actual SQLite close/reopen and a new ViewModel; Android process death is not qualified by that fixture.

Retry reuses tempId, merchant and stored text. Active duplicate retries are rejected within this ViewModel. Dismissal is a local pending-row operation, guarded against active IDs and IDs outside the current model's pending snapshot. A real foreign-merchant row survives a stale callback in the regression. This does not tenant-partition all legacy tables/APIs or establish server authorization, current-account fencing for every old writer, durable cross-process leases or globally exactly-once processing. Two ViewModels/devices, unknown remote completion and receipt reconciliation still need a server/storage contract. Uncertain remote completion must not be described as an atomic Firebase guarantee.

Selected-message editing revalidates current canEdit before entering the editor. Cancellation stops edit/delete work; a cancelled batch does not dispatch another delete. Failed/uncertain deletes remain best-effort local hiding as before; no durable deletion journal is added here.

Evidence: [current qualification](branch75-chat-progress.md), [before/after identities](evidence/continuation/branch75-chat/regressions.json), [actual hosted artifacts](evidence/continuation/branch75-chat/hosted/raw-artifact-verification.json). Client UI/SQLite tests do not qualify deployed Firebase rules or physical-device lifecycle behavior.

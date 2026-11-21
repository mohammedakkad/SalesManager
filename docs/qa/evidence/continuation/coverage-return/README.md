# Seller return recovery — starting b9c978f

Thirty-two targeted cases pass: ReturnCommandsQualificationTest24, ReturnsUiFlowTest5 and ReturnRecoveryRoomTest3. Twenty distinct new regression cases fail against the starting Return ViewModel/UI; one Room case was repeated, producing21 failing executions. The original five targeted cases remain passing. Before/after XML and logs are preserved; a fixture compilation correction is not counted as a production defect.

The decisive real Room case loses the result after a committed partial return, then retries without changing the draft. Before repair the states are:

- First commit: invoices1, remaining sale25, cash125, stock3, eventsCREATE/RETURN.
- Retry: invoices2, remaining sale0, cash100, stock4, eventsCREATE/RETURN/RETURN.

After repair the unchanged retry uses the same invoice/item IDs and timestamps, and the real repository/journal returns the committed result without repeating financial effects. The state remains invoices1, sale25, cash125, stock3 and eventsCREATE/RETURN. The test also exercises a real SQL abort on outbox insertion: return, sale, cash, stock and outbox roll back together, and retry commits once. A file-backed database reopen preserves the result and a fresh draft offers only the remaining quantity. Reopen is not an Android OS process-kill test.

The ViewModel claims submission synchronously, freezes the active draft, prevents modal dismissal from resetting an active request, and cannot resubmit a completed result. Unchanged retries retain immutable identities; actual draft edits create a new intent. Pending client request identity is in memory and is not claimed durable across OS process death. Cancellation propagates without becoming a false failure. Failed reads never substitute zero prior returns; loading ends with a visible error and explicit guarded retry. Numeric guards reject nonfinite/overflowing amounts before confirmation while preserving finite clamping, fractional returns, zero price and the missing-product zero-cost fallback.

No monetary formula, database schema, Admin business logic or production Firebase service was changed by this stage. Network dispatch still belongs to the existing repository/outbox after local commit. Broader historical, server, cross-merchant cache and OS process-lifecycle qualification remains outstanding.

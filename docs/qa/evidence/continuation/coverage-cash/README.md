# Seller cash numeric contracts

Thirteen distinct regressions fail before repair: eight ViewModel boundary/cancellation cases and five real Room mutation cases. After repair all27 targeted cases pass (CashBoxIntegrityTest17, CashFormContractsTest10), with15 new cases and12 retained cases.

The UI rejects NaN, Infinity and numeric overflow before calling the repository. Cancellation propagates without being displayed as an ordinary operation error. Zero, Arabic/Persian digits, decimal values, existing reason labels and trimmed notes retain their behavior. The two public initialization/correction methods now enforce the existing UI rule of finite, nonnegative input.

The shared cash mutation helper validates finite input and verifies the balance after SQLite arithmetic inside its Room transaction, before ledger persistence. Overflow therefore rolls back the balance and any enclosing transfer rather than committing Infinity. Tests prove no changed row or ledger remains for invalid initialization, invalid correction, overflowing finite correction deltas or overflowing cash effects. Existing transfer rollback, offline retry, initialization-once and difference-only correction cases remain passing.

Finite negative cash balances from existing business operations are not clamped or newly prohibited. Monetary formulas, derived debt, schema/version, signing and Admin business logic are unchanged. The mutation helper is consumed by the Seller CashBox repository; this is a required shared integrity boundary. Remote payload validation and complete server/legacy-writer isolation remain separate work.

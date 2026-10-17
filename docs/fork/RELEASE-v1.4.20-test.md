# v1.4.20-test / code1126

Malformed structured tool arguments no longer immediately abort the local turn.
The application returns TOOL_VALIDATION_ERROR with executed=false, the validation
failure and parameter guidance to the model. It can produce a corrected call;
there are at most two schema-correction retries per user turn. Invalid calls never
reach authorization/runner, and corrected calls still pass normal authorization,
scope and optional approval. Security denials are not converted into schema retries.
Prose/embedded examples are not promoted into executable calls. Cancellation is
checked before the next attempt. On retry exhaustion, the UI reports that the
invalid call was not executed; previous tool results remain saved.

Instructions distinguish filter CATEGORY all/text/image/video/apk from extension
suffix (txt without a dot), with the normal allowed limits/types. This addresses
filter=txt by letting the model correct it, not by silently rewriting its JSON.
Validation records use a clear error card and are excluded from future model turns.
The mechanism is shared by local llama.cpp and LiteRT models.

Successful read results reused in the current turn are explicitly marked Cached
using execution metadata, not text found inside a file. The optional history flag
preserves that label when reopening the chat. After two cached repeats of the same
already-successful read, a further identical non-refresh read ends the loop with
an explicit no-final-answer notice and the saved result path. This guard does not
claim the task was completed; it does not prevent explicit refresh requests or
all possible tool loops. A fresh read or successful mutation invalidates relevant
repeat accounting/read cache as before.

The llama CPU selector label is now ARM acceleration (when supported), replacing
Automatic. Internal saved enum auto is unchanged; Compatible CPU remains baseline.
Actual variant/threads remain in diagnostics. Both CPU libraries are byte-identical
to1.4.19-test. No CPU, model-weight, rights, Ubuntu or external-agent changes.

Same com.termux/certificate, code1126 above1125, no schema/history reset. Local test
only; no GitHub publication. Device check: repeat the explicit-path latest-TXT task;
a filter error should be returned to the model, a corrected call should execute,
and repeated cached reads should be identified and bounded.

> Historical experimental feature: removed from the current v1.5.22-test source/APK. This document is retained as research evidence, not current functionality. See [removal notes](RELEASE-v1.5.22-test.md).

> Historical proposal/test notes, superseded by [v1.5.21-test](RELEASE-v1.5.21-test.md). LoRA was cancelled. Referenced training scripts belong to archived commit9ffa1501 and are not present in the current app source.

# Private LoRA pilot — v1.5.20-test

Status: import/loading support, NOT a trained assistant adapter. No trained Qwen or Gemma adapter is bundled. Public local AI remains disabled; this private build is not a GitHub release.

In local session/preset Settings choose **LoRA → Import GGUF**, with a llama.cpp base selected. The app copies the adapter using its file picker and cancellable import progress. One adapter, fixed scale 1.0, CPU only; Android JNI, main Termux runner and dedicated Ubuntu runner are supported by the loader. LiteRT/NPU adapters are disabled pending a verified SDK/model path. Switching adapter reloads the engine with the same saved chat and an empty inference cache. Off restores the base without altering its file. Importing/loading does not train weights.

The importer checks bounded GGUF v3 metadata (`general.type=adapter`, `adapter.type=lora`), copies at most 256 MiB, records SHA-256 and binds the selection to the selected base SHA-256. Files are copied to the app-managed models directory; registry metadata stays in a separate adapters directory. The service derives paths from validated IDs and verifies size/hash/header before loading. The native loader checks architecture/tensor compatibility. Failed attachment is an error, never silent inference without the requested adapter.

**The base binding is user-declared.** Neither the importer nor compatible tensor shapes prove the adapter was trained against that exact base or that its behavior is correct. Use weights with a known matching training manifest. Import only trusted adapters. A `.gguf` extension alone is not a LoRA. Synthetic fixtures used to test the loader are not trained adapters and must not be offered as useful Qwen weights.

## Training goal and current draft

The goal is correct use of our existing tools, not new privileges: resolve phone paths, find screenshots, analyze APKs, run permitted shell/build commands in **jobs-local-ai**, install needed components within the chosen approval policy, read back writes, verify deletions, inspect failures and validate produced APKs. A text-only Qwen can locate an image but cannot gain vision from LoRA. LoRA itself adds weights and compute, not prompt tokens; schemas/current rights/results still use context. No measured speed improvement is claimed.

`scripts/local-ai-training/ExportProfiles.java` exports the current app's Qwen-native brief systems and actual schemas for full/confirmed/read/project/chat profiles. `prepare-dataset.py` builds a **synthetic draft** of scenario-separated train/validation episodes; tool outputs are illustrative, not captured device evidence. Generated data belongs in ignored `.verification/`, not the APK. No auth files, personal chat histories, SSH keys, signing keys, device reports or project source files are read by the generator.

Example after compiling ExportProfiles against the current staged app classes and org.json/android dependencies:

```sh
python3 scripts/local-ai-training/prepare-dataset.py \
  --profiles .verification/local-ai-v1520/training-profiles.json \
  --output .verification/local-ai-v1520/training
```

Episodes cover APK discovery/inspection, archive tools, missing dependencies, read-mode installation approval/refusal, failed builds/tests, artifact checks, write/readback, delete verification/failure, screenshot discovery without fictitious vision, ambiguous files, storage denial, chat/project restrictions, guide-vs-install distinction and untrusted file instructions. These examples are a starting point for manual review and expansion, not enough evidence of trained model quality. Validation scenarios are held out by task; eight variants of one template are not independent behavior evidence.

## Before actual training

1. Pin the exact original trainable Qwen3.5-2B base/revision and its relation to the installed quantized GGUF. Do not train the tiny loader fixture or call its handcrafted matrices learned weights.
2. Inventory supported target tensors in that base, and choose a small pilot rank only after confirming the architecture. Qwen3.5 has hybrid blocks; do not assume a generic Llama target list fits.
3. Render episodes using the same Qwen tool-call/tool-response format, role boundaries and disabled-thinking suffix as the app. Verify tokenizer/template parity and loss masking. JSONL here is episode data, not a trainer-ready token dataset. Assistant tool-call targets need serialization through the app-compatible native protocol. Do not train illustrative `<synthetic-digest>` values as real verification evidence.
4. Use an isolated training environment. This session has not installed training packages, downloaded original training weights or started training. Torch/Transformers/PEFT are not currently available in the inspected Python environment. Phone inference tests are not evidence that practical training will fit this hardware.
5. Evaluate the base vs adapter on held-out RU/EN requests and actual tools: task success, exact paths, wrong-environment writes, unauthorized commands, approval refusal, false completion, loops, prompt injection, latency and memory. Never select on loss alone.
6. Convert supported PEFT weights with the pinned upstream llama.cpp `convert_lora_to_gguf.py`, record base/adapter hashes and training configuration; import only after tensor checks and behavior evaluation. Current loader scale is fixed at 1.0.

Application permissions remain enforced outside the model. Full access plus unrestricted paths enables shell in the selected tool environment; confirmations still follow the session option. Read-only never gains arbitrary shell from training. Its allowlisted `install` tool requires enabled confirmations and explicit application APT-plan approval. Main Termux access is opt-in; no fallback to the working Ubuntu. PRoot is not an Android security sandbox.

## Next preparation checkpoint

Upstream metadata was inspected separately in `.verification/lora-training/`: Qwen/Qwen3.5-2B revision `15852e8c16360a2fea060d615a32b45270f8a8fc`, checkpoint index total4,548,144,832 bytes. The pinned quantizer card names this upstream repository but does not identify its exact source revision; matching dimensions are not exact provenance. No original training weights downloaded.

`ExportTrainingExamples.java` serializes each next-assistant target through production `LocalAiQwenProtocol.call/render`, including tool responses and preceding history. The draft22 episodes produce30 training and23 held-out assistant-step examples; steps from the same episode stay together. These are53 steps from22 synthetic tasks, not53 independent successful device tasks. Export checks verify split separation, nonempty targets and native protocol rendering. End-to-end native/HF tokenizer parity, assistant-only loss masks and training are still pending. Do not use a generic chat template without checking those boundaries.

Checkpoint index contains72 ordinary text MLP matrices (up/down/gate across24 layers), candidate pilot targets that avoid assuming a generic attention projection mapping fits hybrid Qwen. Shapes have not yet been inspected in original tensor data and rank8 is only a proposal. CPU PyTorch wheel availability on ARM64/Python3.14 was checked, not installed or validated in this environment. Approximately8GiB available memory is a point-in-time observation, not proof that training fits. Resource choice (phone-limited pilot versus inspecting server capacity) is pending; no server access or paid resources used.

Automatic learning is a future controlled pipeline: isolated test tasks → application execution → independent outcome checks → reviewed training examples → separate candidate training → held-out comparison and explicit promotion. Chat replies alone do not update weights. Do not train on self-asserted successes, user auth/history/secrets, failed/unverified outcomes labeled successful or the validation split. No autonomous training/promotion loop is implemented by this checkpoint.

# v1.4.22-test — CPU settings for local AI

Session and launch-preset settings now open Engine settings with exactly two controls: Processor mode (ARM acceleration / Compatible CPU) and CPU threads (Auto / 2 / 4 / 6 / 8, restricted to available processors).

Auto preserves the previous maximum of four threads. Manual values are clamped again in the inference service. Changing threads on a loaded idle session reloads the model with the same chat, using the existing lifecycle guards. No permissions, project boundaries, history or other agents are changed. CPU controls are for llama.cpp; LiteRT retains its existing settings.

Compatible update: com.termux, versionCode1128, existing certificate. Optional v8 profile field localThreads defaults to Auto when absent. No data reset required. No GitHub publication requested.

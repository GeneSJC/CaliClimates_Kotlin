# Project specific R8 / ProGuard rules for release builds. The release build type lists this file.
# The app keeps no classes by name (no reflection, no JavaScript interface); the libraries it uses ship their own
# rules. If a release build ever crashes with a missing class or method that the debug build does not, add a
# -keep rule for it here (or in src/main/keepRules/rules.keep) and rebuild.

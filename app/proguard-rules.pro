# Breeze — keep rules for the release build (debug isn't minified).
# None are needed so far: the manifest components keep their names through AAPT's
# generated rules, DataStore ships its own protobuf rules, and the enum names saved
# in DataStore JSON come from constructor strings, which R8 doesn't rename.

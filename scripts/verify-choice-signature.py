#!/usr/bin/env python3
"""
Verify our signing matches Choice Bank doc example.
Doc example: locale=en_KE, params.name=Tester, requestId, salt, sender, senderKey, timestamp
Expected signature: cdfd996e7e5ca655d3fa663db03abe63b852669f04e1f82fda9b473f606a11
"""
import hashlib

# From Choice Bank auth doc example (before removing senderKey)
# String to sign: alphabetically sorted key=value&
# Keys: locale, params.name, requestId, salt, sender, senderKey, timestamp
doc_example = (
    "locale=en_KE&params.name=Tester&requestId=APPREQ00990320fed02000&"
    "salt=QcEwsZ123da&sender=client1&senderKey=yourkey&timestamp=1650533105687"
)
expected_sig = "cdfd996e7e5ca655d3fa663db03abe63b852669f04e1f82fda9b473f606a11"
actual_sig = hashlib.sha256(doc_example.encode("utf-8")).hexdigest()
print("Choice Bank doc example verification:")
print(f"  String: {doc_example[:80]}...")
print(f"  Expected signature: {expected_sig}")
print(f"  Our SHA-256:        {actual_sig}")
print(f"  Match: {actual_sig == expected_sig}")
print()

# Test with our empty params (no params in string)
our_string = "locale=en_KE&requestId=TEST123&salt=abc123&sender=vycein&senderKey=9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j&timestamp=1771230691000"
print("Our empty-params string format:")
print(f"  {our_string[:100]}...")
print(f"  SHA-256: {hashlib.sha256(our_string.encode('utf-8')).hexdigest()}")

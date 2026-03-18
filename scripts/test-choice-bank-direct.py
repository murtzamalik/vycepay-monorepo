#!/usr/bin/env python3
"""
Direct test of Choice Bank API with sender ID and private key.
Usage: python scripts/test-choice-bank-direct.py
Env: CHOICE_BANK_SENDER_ID, CHOICE_BANK_PRIVATE_KEY (or passed as args)
"""
import hashlib
import json
import os
import sys
import time
import uuid

try:
    import urllib.request
except ImportError:
    import urllib2 as urllib  # py2 compat
    urllib.request = urllib

BASE_URL = "https://baas-pilot.choicebankapi.com"
SENDER_ID = os.environ.get("CHOICE_BANK_SENDER_ID", "vycein")
PRIVATE_KEY = os.environ.get("CHOICE_BANK_PRIVATE_KEY", "9x9euCAYsXxBaw02jzzrBiHGisDVVtf8dMNfhpd17BcZxLK13G56KvjFY3bx69j")


def flatten_for_signing(prefix, obj):
    """Flatten nested dict to key=value for signing. Keys use dot notation."""
    out = {}
    if isinstance(obj, dict):
        for k, v in obj.items():
            key = f"{prefix}.{k}" if prefix else k
            if isinstance(v, dict):
                out.update(flatten_for_signing(key, v))
            elif v is not None:
                out[key] = str(v)
    return out


def sign_request(request_id, timestamp, salt, params, sender_id, private_key):
    """Build string-to-sign and compute SHA-256 hex signature."""
    flat = {
        "locale": "en_KE",
        "requestId": request_id,
        "salt": salt,
        "sender": sender_id,
        "timestamp": str(timestamp),
    }
    if params:
        flat.update(flatten_for_signing("params", params))
    else:
        # Choice Bank: params must appear in flattened string; empty = {}
        flat["params"] = "{}"
    flat["senderKey"] = private_key
    # Sort keys ASCII, join key=value&
    parts = []
    for k in sorted(flat.keys()):
        v = flat.get(k) or ""
        parts.append(f"{k}={v}")
    string_to_sign = "&".join(parts)
    return hashlib.sha256(string_to_sign.encode("utf-8")).hexdigest()


def post(path, params=None):
    """POST signed request to Choice Bank."""
    params = params or {}
    request_id = f"TEST-{uuid.uuid4().hex[:16]}"
    timestamp = int(time.time() * 1000)
    salt = uuid.uuid4().hex[:12]
    signature = sign_request(request_id, timestamp, salt, params, SENDER_ID, PRIVATE_KEY)
    body = {
        "requestId": request_id,
        "sender": SENDER_ID,
        "locale": "en_KE",
        "timestamp": timestamp,
        "salt": salt,
        "signature": signature,
        "params": params,
    }
    url = f"{BASE_URL.rstrip('/')}/{path.lstrip('/')}"
    req = urllib.request.Request(
        url,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode())


def main():
    print("=== Choice Bank API Direct Test ===")
    print(f"Base URL: {BASE_URL}")
    print(f"Sender ID: {SENDER_ID}")
    print(f"Private Key: {'*' * 8}...{PRIVATE_KEY[-4:] if len(PRIVATE_KEY) >= 4 else '****'}")
    print()

    # Test 1: getBankCodes (simple, no params)
    print("[1] POST staticData/getBankCodes (empty params)...")
    try:
        resp = post("staticData/getBankCodes", {})
        code = resp.get("code", "")
        msg = resp.get("msg", "")
        data = resp.get("data", {})
        if code == "00000":
            print(f"  OK: code={code}, msg={msg}")
            if data:
                print(f"  Data keys: {list(data.keys()) if isinstance(data, dict) else type(data)}")
                if isinstance(data, dict) and "banks" in data:
                    banks = data.get("banks", [])
                    print(f"  Banks count: {len(banks)}")
                    if banks:
                        print(f"  First bank sample: {banks[0] if isinstance(banks[0], dict) else banks[:1]}")
            else:
                print("  Data: (empty)")
        else:
            print(f"  FAIL: code={code}, msg={msg}")
            print(f"  Full response: {json.dumps(resp, indent=2)[:500]}")
    except Exception as e:
        print(f"  ERROR: {e}")
        if hasattr(e, "read"):
            try:
                print(e.read().decode())
            except Exception:
                pass

    # Test 2: Get enumerations (another simple endpoint if available)
    print()
    print("[2] POST appendix/getEnumerations (if available)...")
    try:
        resp = post("appendix/getEnumerations", {})
        code = resp.get("code", "")
        msg = resp.get("msg", "")
        if code == "00000":
            print(f"  OK: code={code}")
            data = resp.get("data", {})
            if isinstance(data, dict):
                print(f"  Keys: {list(data.keys())[:10]}")
        else:
            print(f"  Response: code={code}, msg={msg}")
    except urllib.error.HTTPError as e:
        print(f"  HTTP {e.code}: {e.reason}")
        try:
            body = e.read().decode()
            print(f"  Body: {body[:300]}")
        except Exception:
            pass
    except Exception as e:
        print(f"  ERROR: {e}")

    print()
    print("=== Done ===")


if __name__ == "__main__":
    main()

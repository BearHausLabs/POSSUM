---
name: Hardware Test Page Update
overview: Add test UI sections for the 4 newly added POSSUM device types (Keylock, POS Keyboard, MSR, Tone Indicator) to the URSA backoffice hardware test page, including service methods, TypeScript interfaces, and UI cards for each. Scanner and Scale are excluded -- they were already in POSSUM before these additions.
todos:
  - id: interfaces
    content: "Add new TypeScript interfaces to possum.service.ts: KeylockPosition, CardData, ToneRequest"
    status: completed
  - id: service-keylock
    content: "Add Keylock service methods: getKeylockPosition(), reconnectKeylock()"
    status: completed
  - id: service-keyboard
    content: "Add POS Keyboard service methods: connectKeyboardEvents() (SSE), reconnectKeyboard()"
    status: completed
  - id: service-msr
    content: "Add MSR service methods: readCard(), cancelCardRead(), reconnectMsr()"
    status: completed
  - id: service-tone
    content: "Add Tone Indicator service methods: playTone(), reconnectToneIndicator()"
    status: completed
  - id: ui-cards
    content: Add 4 new UI cards to hardware-test.ts template (Keylock, POS Keyboard, MSR, Tone Indicator)
    status: completed
  - id: signals-handlers
    content: Add signals, handler methods, and status helpers for all 4 new device types in hardware-test.ts
    status: completed
  - id: build-verify
    content: Build and verify no TypeScript/lint errors
    status: completed
isProject: false
---

# Add New Device Tests to Hardware Test Page

## Scope

The hardware test page at [hardware-test.ts](c:\Users\regadmin\Documents\GitHub\URSA\apps\backoffice\src\app\pages\hardware-test\hardware-test.ts) currently supports Printer, Cash Drawer, MICR/Check, and Line Display. Four newly added POSSUM device types need test cards: **Keylock, POS Keyboard, MSR, and Tone Indicator**. Scanner and Scale are excluded (they already existed in POSSUM).

## Files to Modify

1. **[possum.service.ts](c:\Users\regadmin\Documents\GitHub\URSA\apps\backoffice\src\app\pages\service\possum.service.ts)** -- Add interfaces and service methods for each new device
2. **[hardware-test.ts](c:\Users\regadmin\Documents\GitHub\URSA\apps\backoffice\src\app\pages\hardware-test\hardware-test.ts)** -- Add UI cards, signals, and handler methods for each new device

## New Interfaces (possum.service.ts)

```typescript
// Keylock
export type KeylockPosition = 'LOCKED' | 'NORMAL' | 'SUPERVISOR' | 'UNKNOWN';

// MSR
export interface CardData {
  track1Data?: string;
  track2Data?: string;
  track3Data?: string;
  track4Data?: string;
}

// Tone Indicator
export interface ToneRequest {
  pitch1: number;
  duration1: number;
  volume1: number;
  pitch2?: number;
  duration2?: number;
  volume2?: number;
  interToneWait?: number;
}
```

## New Service Methods (possum.service.ts)

- **Keylock:** `getKeylockPosition()` (GET /v1/keylock/position), `reconnectKeylock()` (POST /v1/keylock/reconnect)
- **POS Keyboard:** `connectKeyboardEvents()` (SSE via EventSource to /v1/poskeyboard/events), `reconnectKeyboard()` (POST /v1/poskeyboard/reconnect)
- **MSR:** `readCard()` (GET /v1/msr/read), `cancelCardRead()` (DELETE /v1/msr/read), `reconnectMsr()` (POST /v1/msr/reconnect)
- **Tone Indicator:** `playTone(request: ToneRequest)` (POST /v1/toneindicator/sound), `reconnectToneIndicator()` (POST /v1/toneindicator/reconnect)

## New UI Cards (hardware-test.ts)

Each card follows the existing pattern: header with device name + status badge, action buttons, and result message area.

### 1. Keylock Card (col-12 lg:col-6)

- "Read Position" button -- calls `getKeylockPosition()`, shows LOCKED/NORMAL/SUPERVISOR
- "Reconnect" button
- Result showing current position with color-coded badge

### 2. POS Keyboard Card (col-12 lg:col-6)

- "Start Listening" / "Stop Listening" toggle -- opens/closes EventSource SSE stream
- Live event log showing key presses in a monospace scrollable area (similar to test results panel)
- "Reconnect" button

### 3. MSR (Card Reader) Card (col-12 lg:col-6)

- "Read Card" button -- calls `readCard()`, shows track data
- "Cancel Read" button
- "Reconnect" button
- Result showing track1-4 data

### 4. Tone Indicator Card (col-12 lg:col-6)

- Preset tone buttons: "Success Beep", "Error Beep", "Alert Tone"
- Each preset maps to a `ToneRequest` with appropriate pitch/duration/volume values
- "Reconnect" button
- Result message

## Status Helpers

Add to existing status helpers section:

- `getKeylockStatus()` -- looks for category 'Keylock'
- `getKeyboardStatus()` -- looks for category 'POSKeyboard'
- `getMsrStatus()` -- looks for category 'MSR'
- `getToneStatus()` -- looks for category 'ToneIndicator'

## Layout

The new cards will be inserted between the existing MICR card and the Sale Simulator card, maintaining the 2-column grid layout (col-12 lg:col-6). The order will be:

1. Printer (existing)
2. Cash Drawer (existing)
3. MICR/Check Reader (existing)
4. MSR / Card Reader (new)
5. Keylock (new)
6. POS Keyboard (new)
7. Tone Indicator (new)
8. Sale Simulator (existing)
9. Line Display Automated Tests (existing)
10. Pole Display (existing)
11. Device Health Monitor (existing)

## Notes

- The POS Keyboard SSE stream uses native `EventSource` (not HttpClient) since Angular HttpClient doesn't natively support SSE. The EventSource URL will be constructed from the base URL.
- The MSR "read" endpoint blocks until a card is swiped (long-polling style), so the UI should show a loading/waiting state and offer a cancel button.
- The base URL is currently `http://10.11.35.5:8080/v1` -- the new service methods will use the same `baseUrl`.


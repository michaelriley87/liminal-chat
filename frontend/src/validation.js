export const MAX_NAME_LENGTH = 24
export const MAX_MESSAGE_LENGTH = 500

export const ROOM_CODE_PATTERN = /^[A-Z0-9]{5}$/

export function normaliseDisplayName(value) {
  return typeof value === 'string' ? value.trim() : ''
}

export function getDisplayNameError(name) {
  if (!name) {
    return 'Enter a display name.'
  }

  if (name.length > MAX_NAME_LENGTH) {
    return `Display names must be ${MAX_NAME_LENGTH} characters or fewer.`
  }

  return ''
}

export function normaliseRoomCode(value) {
  return typeof value === 'string' ? value.trim().toUpperCase() : ''
}

export function isValidRoomCode(roomCode) {
  return ROOM_CODE_PATTERN.test(roomCode)
}

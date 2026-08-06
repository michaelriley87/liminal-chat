import { describe, expect, it } from 'vitest'

import {
  getDisplayNameError,
  isValidRoomCode,
  MAX_NAME_LENGTH,
  normaliseDisplayName,
  normaliseRoomCode,
} from './validation'

describe('display-name validation', () => {
  it('normalises and accepts a name at the maximum length', () => {
    const name = normaliseDisplayName(`  ${'a'.repeat(MAX_NAME_LENGTH)}  `)

    expect(name).toHaveLength(MAX_NAME_LENGTH)
    expect(getDisplayNameError(name)).toBe('')
  })

  it('rejects blank and overlength names', () => {
    expect(getDisplayNameError(normaliseDisplayName('   '))).toBe('Enter a display name.')
    expect(getDisplayNameError('a'.repeat(MAX_NAME_LENGTH + 1))).toContain('24 characters')
  })
})

describe('room-code validation', () => {
  it('trims and uppercases valid room codes', () => {
    const roomCode = normaliseRoomCode(' ab12c ')

    expect(roomCode).toBe('AB12C')
    expect(isValidRoomCode(roomCode)).toBe(true)
  })

  it.each(['ABCD', 'ABCDEF', 'AB-12', '', null])('rejects invalid room code %s', (value) => {
    expect(isValidRoomCode(normaliseRoomCode(value))).toBe(false)
  })
})

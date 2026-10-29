import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { downloadBlob, openBlobInNewTab } from '../download'

describe('download utils', () => {
  let originalWindow
  let originalDocument
  let mockElement

  beforeEach(() => {
    vi.useFakeTimers()
    originalWindow = globalThis.window
    originalDocument = globalThis.document

    mockElement = {
      href: '',
      setAttribute: vi.fn(),
      click: vi.fn()
    }

    globalThis.window = {
      URL: {
        createObjectURL: vi.fn().mockReturnValue('blob:http://localhost/fake-uuid'),
        revokeObjectURL: vi.fn()
      },
      open: vi.fn()
    }

    globalThis.document = {
      createElement: vi.fn().mockReturnValue(mockElement),
      body: {
        appendChild: vi.fn(),
        removeChild: vi.fn()
      }
    }
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
    globalThis.window = originalWindow
    globalThis.document = originalDocument
  })

  describe('downloadBlob', () => {
    it('does nothing if data is null or undefined', () => {
      downloadBlob(null, 'test.pdf')
      downloadBlob(undefined, 'test.pdf')
      expect(window.URL.createObjectURL).not.toHaveBeenCalled()
    })

    it('creates a link, triggers click, and immediately revokes the object URL', () => {
      const blob = new Blob(['sample content'], { type: 'application/pdf' })
      downloadBlob(blob, 'reporte.pdf')

      expect(window.URL.createObjectURL).toHaveBeenCalledWith(blob)
      expect(mockElement.setAttribute).toHaveBeenCalledWith('download', 'reporte.pdf')
      expect(mockElement.click).toHaveBeenCalled()
      expect(document.body.appendChild).toHaveBeenCalledWith(mockElement)
      expect(document.body.removeChild).toHaveBeenCalledWith(mockElement)
      expect(window.URL.revokeObjectURL).toHaveBeenCalledWith('blob:http://localhost/fake-uuid')
    })
  })

  describe('openBlobInNewTab', () => {
    it('does nothing if data is null or undefined', () => {
      openBlobInNewTab(null)
      openBlobInNewTab(undefined)
      expect(window.URL.createObjectURL).not.toHaveBeenCalled()
      expect(window.open).not.toHaveBeenCalled()
    })

    it('opens blob in new tab and revokes object URL after 60,000ms', () => {
      const blob = new Blob(['preview content'], { type: 'application/pdf' })
      openBlobInNewTab(blob)

      expect(window.URL.createObjectURL).toHaveBeenCalledWith(blob)
      expect(window.open).toHaveBeenCalledWith('blob:http://localhost/fake-uuid', '_blank')
      expect(window.URL.revokeObjectURL).not.toHaveBeenCalled()

      // Advance by 59,999ms - should still not be revoked
      vi.advanceTimersByTime(59999)
      expect(window.URL.revokeObjectURL).not.toHaveBeenCalled()

      // Advance 1ms more to reach 60,000ms
      vi.advanceTimersByTime(1)
      expect(window.URL.revokeObjectURL).toHaveBeenCalledTimes(1)
      expect(window.URL.revokeObjectURL).toHaveBeenCalledWith('blob:http://localhost/fake-uuid')
    })

    it('wraps ArrayBuffer or string into Blob with given mimeType', () => {
      const rawText = 'hello world'
      openBlobInNewTab(rawText, 'text/plain')

      expect(window.URL.createObjectURL).toHaveBeenCalled()
      expect(window.open).toHaveBeenCalledWith('blob:http://localhost/fake-uuid', '_blank')

      vi.advanceTimersByTime(60000)
      expect(window.URL.revokeObjectURL).toHaveBeenCalledTimes(1)
    })
  })
})

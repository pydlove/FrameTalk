export type ThemePreference = 'system' | 'light' | 'dark'

export interface SegmentedItem<T extends string> {
  value: T
  label: string
}

export type SettingsSection =
  | 'general'
  | 'security'
  | 'appearance'
  | 'about'

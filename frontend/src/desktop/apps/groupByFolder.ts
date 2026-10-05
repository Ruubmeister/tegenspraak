/** Folder names in order of first appearance, so case authors control the order. */
export function folderNames<T extends { folder: string }>(items: T[]): string[] {
  return [...new Set(items.map((item) => item.folder))]
}

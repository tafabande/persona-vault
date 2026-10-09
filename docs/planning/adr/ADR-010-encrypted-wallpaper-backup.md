# ADR-010: Encrypted backup of user-imported wallpapers

**Status:** Accepted

User-imported carousel wallpapers are private assets. They are stored locally under the app-private wallpaper directory and backed up only through the established encrypted object-storage envelope used for private image assets. Firestore stores metadata and an encrypted-object reference, never image bytes or a public image URL.

Each wallpaper keeps its generated stable ID. Its title, alignment, carousel order, and active-state metadata are stored under the authenticated account namespace. Restore verifies and decrypts the binary before adding it to the local carousel. Built-in generative presets are not uploaded.

Deleting a custom wallpaper removes its remote metadata and encrypted object only after the local user explicitly deletes it.

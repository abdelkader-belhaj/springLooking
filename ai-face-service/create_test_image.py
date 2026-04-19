import cv2
import numpy as np

# Create a simple test image (solid color with a face-like circle)
img = np.ones((200, 200, 3), dtype=np.uint8) * 255

# Draw a circle (simple "face" simulation)
cv2.circle(img, (100, 100), 50, (0, 0, 255), -1)
cv2.circle(img, (85, 85), 8, (0, 0, 0), -1)
cv2.circle(img, (115, 85), 8, (0, 0, 0), -1)

# Save as JPEG
cv2.imwrite('test_face.jpg', img)
print("✓ Created test_face.jpg (200x200 JPEG with simple face)")

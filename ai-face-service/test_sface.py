from deepface import DeepFace

result = DeepFace.represent(
    img_path='test_face.jpg',
    model_name='SFace',
    detector_backend='opencv',
    enforce_detection=False,
)

print('ok', len(result[0]['embedding']))

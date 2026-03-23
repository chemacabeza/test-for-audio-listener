from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import JSONResponse
from faster_whisper import WhisperModel
import os
import tempfile
import logging

app = FastAPI(title="Shadow Whisper API", version="1.0")

# Configure basic logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

logger.info("Initializing offline `faster-whisper` 'base' model... (CPU INT8 Quantization)")
# The 'base' model handles impressive accuracy while maintaining incredibly fast CPU processing speeds.
model = WhisperModel("base", device="cpu", compute_type="int8", local_files_only=True)
logger.info("Model loaded into RAM successfully!")

@app.post("/v1/audio/transcriptions")
async def transcribe_audio(
    file: UploadFile = File(...),
    model_name: str = Form(default="whisper-1", alias="model")
):
    """
    Mathematically mimics the OpenAI Audio Transcription Endpoint!
    """
    tmp_path = None
    try:
        logger.info(f"Received request: {file.filename}")
        
        # Save the multipart binary audio payload to a temporary local file
        ext = os.path.splitext(file.filename)[1]
        with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
            tmp.write(await file.read())
            tmp_path = tmp.name

        logger.info(f"Executing offline transcription on {tmp_path}...")
        
        # Perform offline inference
        segments, info = model.transcribe(tmp_path, beam_size=5)
        
        transcribed_text = " ".join(segment.text for segment in segments).strip()
        logger.info(f"Transcription complete! Yielded {len(transcribed_text)} characters.")

        # Exactly replicate the expected JSON format!
        return JSONResponse(content={"text": transcribed_text})

    except Exception as e:
        logger.error(f"Inference error: {str(e)}")
        return JSONResponse(
            status_code=500, 
            content={"error": {"message": str(e), "type": "offline_inference_error"}}
        )
    finally:
        # Guarantee cleanup of temporary OS files
        if tmp_path and os.path.exists(tmp_path):
            os.remove(tmp_path)

@app.get("/health")
def health_check():
    return {"status": "online", "model": "base"}

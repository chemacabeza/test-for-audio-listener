FROM python:3.10

WORKDIR /app

# Whisper intrinsically relies on FFmpeg OS binaries to process and downsample diverse audio containers (mp3, wav, flac).
RUN apt-get update && apt-get install -y ffmpeg build-essential python3-dev && rm -rf /var/lib/apt/lists/*

# Install rigid Python dependencies
COPY requirements.txt .
RUN pip install --upgrade pip
RUN pip install --no-cache-dir -r requirements.txt

# Copy FastAPI script
COPY main.py .

# CRITICAL: Pre-download the Neural Network HuggingFace weights natively during the Docker Image BUILD phase!
# This strictly guarantees that when the container boots, it never attempts to hit the internet!
RUN python -c "from faster_whisper import WhisperModel; WhisperModel('base', device='cpu', compute_type='int8')"

EXPOSE 8000

# Expose Uvicorn HTTP server
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

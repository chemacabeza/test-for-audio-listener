'use client';

import { useState, useRef } from 'react';

interface FileUploadProps {
    onFileSelected: (file: File) => void;
    disabled?: boolean;
}

const ACCEPTED_TYPES = [
    'audio/mpeg',
    'audio/wav',
    'audio/ogg',
    'audio/flac',
    'audio/mp4',
    'audio/webm',
    'audio/x-m4a',
];

export default function FileUpload({ onFileSelected, disabled }: FileUploadProps) {
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [dragActive, setDragActive] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);

    const validateFile = (file: File): boolean => {
        setError(null);

        if (!ACCEPTED_TYPES.includes(file.type)) {
            setError(`Unsupported file type: ${file.type}. Accepted: MP3, WAV, OGG, FLAC, M4A, MP4, WebM`);
            return false;
        }

        const maxSize = 25 * 1024 * 1024; // 25MB
        if (file.size > maxSize) {
            setError(`File too large: ${(file.size / (1024 * 1024)).toFixed(1)} MB. Maximum: 25 MB`);
            return false;
        }

        return true;
    };

    const handleFileChange = (file: File) => {
        if (validateFile(file)) {
            setSelectedFile(file);
        } else {
            setSelectedFile(null);
        }
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setDragActive(false);
        if (e.dataTransfer.files?.[0]) {
            handleFileChange(e.dataTransfer.files[0]);
        }
    };

    const handleSubmit = () => {
        if (selectedFile) {
            onFileSelected(selectedFile);
            setSelectedFile(null);
            if (inputRef.current) inputRef.current.value = '';
        }
    };

    return (
        <div className="upload-card">
            <h3>📁 Upload Audio File</h3>

            <div
                className={`drop-zone ${dragActive ? 'drop-zone-active' : ''}`}
                onDragOver={(e) => { e.preventDefault(); setDragActive(true); }}
                onDragLeave={() => setDragActive(false)}
                onDrop={handleDrop}
                onClick={() => inputRef.current?.click()}
            >
                <p>📎 Drag & drop an audio file here, or click to browse</p>
                <p className="text-muted">Supported: MP3, WAV, OGG, FLAC, M4A, WebM (max 25 MB)</p>
                <input
                    ref={inputRef}
                    type="file"
                    accept="audio/*"
                    onChange={(e) => e.target.files?.[0] && handleFileChange(e.target.files[0])}
                    hidden
                />
            </div>

            {error && <p className="error-text">{error}</p>}

            {selectedFile && (
                <div className="file-info">
                    <p>
                        <strong>{selectedFile.name}</strong> — {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB
                    </p>
                    <button
                        className="btn btn-primary"
                        onClick={handleSubmit}
                        disabled={disabled}
                    >
                        📤 Submit for Transcription
                    </button>
                </div>
            )}
        </div>
    );
}

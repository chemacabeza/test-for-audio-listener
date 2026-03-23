'use client';

import { useState, useRef, useCallback } from 'react';

interface AudioRecorderProps {
    onRecordingComplete: (file: File) => void;
    disabled?: boolean;
}

export default function AudioRecorder({ onRecordingComplete, disabled }: AudioRecorderProps) {
    const [isRecording, setIsRecording] = useState(false);
    const [audioUrl, setAudioUrl] = useState<string | null>(null);
    const [recordedBlob, setRecordedBlob] = useState<Blob | null>(null);
    const [duration, setDuration] = useState(0);
    const mediaRecorderRef = useRef<MediaRecorder | null>(null);
    const chunksRef = useRef<Blob[]>([]);
    const timerRef = useRef<NodeJS.Timeout | null>(null);

    const startRecording = useCallback(async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const mediaRecorder = new MediaRecorder(stream, {
                mimeType: MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : 'audio/ogg',
            });
            mediaRecorderRef.current = mediaRecorder;
            chunksRef.current = [];
            setDuration(0);
            setAudioUrl(null);
            setRecordedBlob(null);

            mediaRecorder.ondataavailable = (e) => {
                if (e.data.size > 0) chunksRef.current.push(e.data);
            };

            mediaRecorder.onstop = () => {
                const blob = new Blob(chunksRef.current, { type: mediaRecorder.mimeType });
                setRecordedBlob(blob);
                setAudioUrl(URL.createObjectURL(blob));
                stream.getTracks().forEach((track) => track.stop());
            };

            mediaRecorder.start(250);
            setIsRecording(true);

            timerRef.current = setInterval(() => {
                setDuration((d) => d + 1);
            }, 1000);
        } catch (err) {
            alert('Could not access microphone. Please check permissions.');
            console.error('Microphone access error:', err);
        }
    }, []);

    const stopRecording = useCallback(() => {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
            if (timerRef.current) {
                clearInterval(timerRef.current);
                timerRef.current = null;
            }
        }
    }, []);

    const submitRecording = useCallback(() => {
        if (recordedBlob) {
            const ext = recordedBlob.type.includes('webm') ? 'webm' : 'ogg';
            const file = new File([recordedBlob], `recording-${Date.now()}.${ext}`, {
                type: recordedBlob.type,
            });
            onRecordingComplete(file);
        }
    }, [recordedBlob, onRecordingComplete]);

    const formatDuration = (seconds: number) => {
        const m = Math.floor(seconds / 60).toString().padStart(2, '0');
        const s = (seconds % 60).toString().padStart(2, '0');
        return `${m}:${s}`;
    };

    return (
        <div className="recorder-card">
            <h3>🎙️ Record Audio</h3>

            <div className="recorder-controls">
                {!isRecording ? (
                    <button
                        className="btn btn-record"
                        onClick={startRecording}
                        disabled={disabled}
                    >
                        ⏺ Start Recording
                    </button>
                ) : (
                    <button className="btn btn-stop" onClick={stopRecording}>
                        ⏹ Stop ({formatDuration(duration)})
                    </button>
                )}
            </div>

            {audioUrl && (
                <div className="recorder-preview">
                    <audio controls src={audioUrl} />
                    <button
                        className="btn btn-primary"
                        onClick={submitRecording}
                        disabled={disabled}
                    >
                        📤 Submit for Transcription
                    </button>
                </div>
            )}
        </div>
    );
}

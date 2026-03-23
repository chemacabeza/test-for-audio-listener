import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Audio Listener — Speech-to-Text Transcription',
  description: 'Record or upload audio and get instant transcriptions powered by OpenAI Whisper. Built with Next.js and Spring Boot.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <head>
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap"
          rel="stylesheet"
        />
      </head>
      <body>
        <main>{children}</main>
        <footer className="app-footer">
          <p>Audio Listener — Powered by OpenAI Whisper &amp; Spring Boot</p>
        </footer>
      </body>
    </html>
  );
}

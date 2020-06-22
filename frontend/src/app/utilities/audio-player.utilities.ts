export class AudioPlayerUtilities {
    /**
     * Wrapper for JavaScript playback of an HTML audio player element. Takes care of error handling.
     *
     * @param audio The HTMLAudioElement that should be played.
     */
    static playOnce(audio: HTMLAudioElement) {
        audio.play().catch(reason => console.warn(`Failed to play audio effects due to an error!`)).then(() => {});
    }
}

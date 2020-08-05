export class DataUtilities {
    /**
     * Converts a Base64 encoded string into an object URL of a Blob.
     *
     * @param base64 The base64 encoded string.
     * @param contentType The content type of the data.
     */
    static base64ToUrl(base64: string, contentType: string): string {
        const binary = atob(base64);
        const byteNumbers = new Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            byteNumbers[i] = binary.charCodeAt(i);
        }
        const byteArray = new Uint8Array(byteNumbers);
        const blob = new Blob([byteArray], {type: contentType});
        return window.URL.createObjectURL(blob);
    }
}

export function toJSONOrFail(response: Response): any {
    if (!response.ok) {
        return response.text().then(text => {
            throw new Error(
                `Response was not ok. HTTP code: ${response.status}. Body:\n${text}`)
        })
    }

    return response.json();
}
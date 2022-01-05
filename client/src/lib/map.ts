import {toJSONOrFail} from './requests'

export class SelectedPlace {
    constructor(
        public name: string,
        public lat: number,
        public long: number) {
    }
}

class MapSessionToken {
    readonly token: string;

    constructor() {
        this.token = '' + Math.random() * 10 ** 7;
    }
}

export const MapService = {
    newSessionToken() {
        return new MapSessionToken();
    },

    async fetchSearchResults(query: string, sessionToken: MapSessionToken): Promise<Array<{name: string, entryId: string}>> {
        const resp = fetch(
            'http://localhost:8000/api/map/search?' +
            `session_token=${sessionToken.token}&query=${query}`, {
                credentials: 'include',
            })
        return (await toJSONOrFail(await resp))['entries'];
    },

    async fetchEntryDetails(entryId: string): Promise<SelectedPlace> {
        const resp = fetch(`http://localhost:8000/api/map/details?entry_id=${entryId}`, {
            credentials: 'include',
        })
        return await toJSONOrFail(await resp) as SelectedPlace;
    },

    getStaticMapSrc(place: SelectedPlace, width: number, height: number): string {
        const apiKey = import.meta.env.VITE_GOOGLE_API_KEY;
        return 'https://maps.googleapis.com/maps/api/staticmap?' +
            `key=${apiKey}&markers=${place.lat},${place.long}&zoom=7&size=${width}x${height}`;
    }
}
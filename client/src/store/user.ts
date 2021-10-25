import { writable } from 'svelte/store';

export interface User {
    userId: string,
    name?: string,
    occupation?: string,
    ageDisplay?: string,
    redditUsername?: string,
}

export const user = (function() {
    const {subscribe, set} = writable<User|null>(null);

    return {
        subscribe,
        setUser(user: User) {
            set(user);
        },
        removeUser() {
            set(null);
        },
    };
})();
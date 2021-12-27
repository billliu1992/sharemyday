<script lang="ts">
    import Upload from './Upload.svelte';
    import {User, user} from './store/user';

    fetch('http://localhost:8000/api/user/me', {credentials: 'include'})
        .then(response => response.json())
        .then(userJson => user.setUser(userJson));
</script>

{#if $user}
    Hello {$user.redditUsername}
{:else}
    <a href="http://localhost:8000/auth/reddit/start">
        Login via Reddit
    </a>
{/if}

<Upload />

<style global lang=less>
    @import "common";

    body {
        font-family: system-ui;
    }

    h1, h2, h3 {
        margin: 0;
    }

    textarea,
    input[type="text"] {
        background-color: unset;
        border: 0;
        border-bottom: 1px solid @medium-green;
        color: @medium-green;
        outline: 0;
    }

    .btn {
        background-color: @light-green;
        border: 1px solid @medium-green;
        border-radius: 5px;
        color: @dark-green;
    }
</style>

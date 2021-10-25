<script lang="ts">
    import {User, user} from './store/user';

    fetch('http://localhost:8000/api/me', {credentials: 'include'})
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

<style>
</style>

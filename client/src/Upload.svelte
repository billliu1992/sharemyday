<!-- Make the compiler not rerender each image on change -->
<svelte:options immutable />

<script lang=ts>
    interface Image {
        id: number,
        imageBase64: string,
        describedTime: string,
        description: string,
    }
    let _counter = 0;

    let testMe = '';

    let images: Array<Image> = [];

    function onImageUploaded(event: Event) {
        if (!(event.currentTarget instanceof HTMLInputElement)) {
            return;
        }
        for (const file of event.currentTarget.files) {
            const reader = new FileReader();
            reader.readAsDataURL(file);
            reader.addEventListener('load', () => {
                images = [...images, {
                    imageBase64: reader.result as string,
                    id: _counter++,
                    describedTime: "",
                    description: "",
                }]
            });
        }
        event.currentTarget.value = '';
    }

    function submit() {
        fetch('http://localhost:8000/api/days/new', {
            credentials: 'include',
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                occupation: "",
                occasion: "",
                longitude: 0,
                latitude: 0,
                photos: images.map(image => ({
                    imageBase64: image.imageBase64,
                    description: image.description,
                    describedTime: image.describedTime,
                })),
            }),
        });
    } 
</script>

<input type="text" bind:value={testMe}>

{#each images as image, i (image.id)}
    <img src={images[i].imageBase64} alt="This is a cool thing.">
    <input type="text" on:change|stopPropagation={(e) => image.describedTime = e.currentTarget.value}>
    <textarea on:change|stopPropagation={(e) => image.description = e.currentTarget.value}></textarea>
{/each}

<button on:click={submit}>Submit</button>

<input type="file" multiple accept="image/*" on:change={onImageUploaded}>

<style type="text/scss">
    img {
        max-width: 100px;
        max-height: 100px;
    }
</style>
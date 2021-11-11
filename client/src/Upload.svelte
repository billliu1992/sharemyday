<!-- Make the compiler not rerender each image on change -->
<svelte:options immutable />

<script lang=ts>
    interface Image {
        id: number,
        imageBase64: string,
        when: string,
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
                    when: "",
                    description: "",
                }]
            });
        }
        event.currentTarget.value = '';
    }

    function submit() {
        console.log("I just submitted", images);
    } 
</script>

<input type="text" bind:value={testMe}>

{#each images as image, i (image.id)}
    <img src={images[i].imageBase64} alt="This is a cool thing.">
    <input type="text" on:change|stopPropagation={(e) => image.when = e.currentTarget.value}>
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
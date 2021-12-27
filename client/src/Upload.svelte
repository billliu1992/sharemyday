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

<div class="share-modal">
    <header>
        <h2>Share your day</h2>
        <button type="button">X</button>
    </header>
    <main>
        <div class="title-inputs">
            <span class="input-preamble">A day in the life of</span>
            <span class="input-row">a <input type="text"></span>
            <span class="input-row">in <input type="text"></span>
        </div>

        <div class="image-inputs">
            {#each images as image, i (image.id)}
                <div class="image-entry">
                    <div class="image-container">
                        <img src={images[i].imageBase64} alt="This is a cool thing.">
                    </div>
                    <div class="image-entry-text">
                        <input type="text" on:change={(e) => image.describedTime = e.currentTarget.value}
                            placeholder="Describe when this photo was taken.">
                        <textarea on:change={(e) => image.description = e.currentTarget.value}
                            placeholder="Add any additional details that you’d like to share."></textarea>
                    </div>
                </div>
            {/each}
            <div class="drop-target">
                <div class="drop-border">
                    <div>Drop some photos here</div>
                    <div>or</div>
                    <input type="file" multiple accept="image/*" on:change={onImageUploaded}>
                </div>
            </div>
        </div>
    </main>

    {#if images.length}
        <button class="submit btn" on:click={submit} type="button">
            Share your day with {images.length} photo(s)
        </button>
    {/if}
</div>

<style lang=less>
    @import 'common';

    @content-padding: 36px;

    .share-modal {
        background-color: @background-off-white;
        border: 1px solid @gray;
        border-radius: 5px;
        max-width: 800px;
        width: 100%;
        position: absolute;
        top: 0;
        left: 0;
        min-height: 600px;
        height: 100%;
        overflow: hidden;
        display: flex;
        flex-direction: column;

        @media @phone {
            max-height: 100vh;
        }

        header {
            border-bottom: 1px solid @gray;
            display: flex;
            justify-content: space-between;
            padding: 16px 12px 16px @content-padding;

            h2 {
                font-size: 24px;
                font-weight: normal;
            }

            button {
                background-color: unset;
                border: 0;
                font-size: 16px;
            }
        }

        main {
            padding: 16px @content-padding 54px;
            width: 100%;
            flex: 0 0 540px;
            display: flex;
            flex-direction: column;
            box-sizing: border-box;
            overflow-y: auto;
            overflow-x: hidden;

            @media @phone {
                justify-content: center;
                align-items: center;
                flex: 0 1 100%;
                padding: 16px 0 54px;
                overflow-x: hidden;
            }
            
            .title-inputs {
                font-size: 24px;
                margin-bottom: 16px;

                input {
                    font-size: 24px;
                    outline: 0;
                    width: 9em;
                }

                @media @phone {
                    text-align: center;
                    font-size: 14px;

                    .input-preamble {
                        display: block;
                    }

                    .input-row {
                        display: inline-block;

                        input {
                            width: 5em;
                        }
                    } 
                }
            }

            .image-inputs {
                display: flex;
                flex: 0 1 100%;
                flex-direction: column;

                @media @phone {
                    flex-direction: row;
                    width: 100%;
                    box-sizing: border-box;
                    overflow-x: auto;
                    padding: 0;
                }

                .image-entry {
                    display: flex;
                    flex: 0 0 175px;
                    padding: 24px 0;
                    box-sizing: border-box;
                    border-bottom: 1px solid @gray;

                    @media @phone {
                        flex: 0 0 100%;
                        flex-direction: column;
                        border-bottom: 0;
                        padding: 0 24px;
                    }

                    .image-container {
                        flex: 0 0 175px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        overflow: hidden;

                        @media @phone {
                            flex: 0 0 50%;
                        }

                        img {
                            max-height: 100%;
                            max-width: 100%;
                        }
                    }
                    
                    .image-entry-text {
                        flex: 0 1 100%;
                        display: flex;
                        flex-direction: column;
                        font-size: 14px;
                        margin-left: 16px;

                        @media @phone {
                            margin: 16px 0 0 0;
                        }

                        input {
                            font-size: 14px;
                            line-height: 1.5;
                            margin-bottom: 14px;
                        }

                        textarea {
                            font-size: 14px;
                            height: 100%;
                            resize: vertical;
                            font-family: inherit;
                        }
                    }
                }

                .drop-target {
                    flex: 1 0 175px;
                    width: 100%;
                    box-sizing: border-box;
                    padding: 24px 0 48px 0;

                    @media @phone {
                        flex: 0 0 100%;
                        padding: 0 28px;
                    }

                    .drop-border {
                        border: 1px dashed @medium-green;
                        box-sizing: border-box;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        height: 100%;
                    }
                }
            }
        }

        .submit {
            font-size: 18px;
            line-height: 1.5;
            position: absolute;
            bottom: 10px;
            left: 50%;
            transform: translateX(-50%);
            box-sizing: border-box;
            width: 80%;
            max-width: 500px;
        }
    }
</style>
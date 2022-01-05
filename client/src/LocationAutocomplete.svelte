<script lang=ts>
    import _ from "lodash";
    import {MapService, SelectedPlace} from "./lib/map";

    const sessionToken = MapService.newSessionToken();

    console.log(import.meta.env);

    let opts = [];
    let showAutocomplete = false;
    let showMapPreview = false;
    let locationInput = "";
    let selected: SelectedPlace|null = null;
    let mapPreviewImg: HTMLElement|null = null;

    const onAutoCompleteKeyUp = _.debounce(function (query) {
        if (!query || query.length < 3) {
            return;
        }
        selected = null;
        MapService.fetchSearchResults(query, sessionToken).then(entries => opts = entries);
    }, 200);

    function onAutoCompleteFocus() {
        showAutocomplete = true;
    }

    function onAutoCompleteBlur() {
        showAutocomplete = false;
        if (opts.length) {
            onEntryClick(opts[0]);
        }
    }

    function onEntryClick(opt) {
        MapService.fetchEntryDetails(opt.entryId)
            .then(details => {
                selected = details;
                locationInput = selected.name;
            });
    }

    function onMouseEnter() {
        debouncedMouseLeave.cancel();
        debouncedMouseEnter();
    }
    const debouncedMouseEnter = _.debounce(function() {
        if (!selected) {
            return;
        }
        showMapPreview = true;
    }, 300);

    function onMouseLeave() {
        debouncedMouseEnter.cancel();
        debouncedMouseLeave();
    }
    const debouncedMouseLeave = _.debounce(function() {
        showMapPreview = false;
    }, 100);
</script>

<div class="container" on:mouseenter={() => onMouseEnter()}
    on:mouseleave={() => onMouseLeave()}>
    <input type="text"
        class={"input " + $$props.class}
        placeholder="where?"
        bind:value={locationInput}
        on:blur={() => onAutoCompleteBlur()}
        on:focus={() => onAutoCompleteFocus()}
        on:keyup={e => onAutoCompleteKeyUp(e.currentTarget.value)}>

    <div class="popup"
        class:showAutocomplete={showAutocomplete}
        class:showSelectedMap={showMapPreview}>
        <div class="arrow"></div>

        <div class="autocomplete">
            {#each opts as opt (opt.entryId)}
                <div class="entry" on:mousedown={() => onEntryClick(opt)}>{opt.text}</div>
            {:else}
                {#if locationInput.length < 3}
                    <div class="empty">Please enter a city, region, or country.</div>
                {:else}
                    <div class="no-results">No results found. Please try being less specific</div>
                {/if}
            {/each}
        </div>
        <div class="map">
            {#if selected}
                <img src={MapService.getStaticMapSrc(selected, 200, 200)}
                    alt="A map showing the selected place." />
            {/if}
        </div>
    </div>
</div>

<style lang=less>
    @import 'common';

    .container {
        display: inline-block;
        position: relative;

        .popup {
            border: 1px solid @medium-green;
            border-radius: 5px;
            font-size: 14px;
            background-color: @background-off-white;
            display: none;
            position: absolute;

            @media @not-phone {
                top: 3em;
                left: 50%;
                transform: translateX(-50%);
            }

            @media @phone {
                top: 2.1em;
                right: 0;
            }

            .arrow {
                background-color: @background-off-white;
                width: 8px; 
                height: 8px; 
                border-top: 1px solid @medium-green;
                border-right: 1px solid @medium-green;
                transform: rotate(-45deg);
                position: absolute;
                top: -5px;

                @media @not-phone {
                    left: 50%;
                }
                @media @phone {
                    right: 4.25em;
                }
            }

            .empty,
            .no-results {
                padding: 12px;
            }

            .autocomplete {
                padding-top: 6px;
                border-radius: 5px;
                overflow: hidden;
                width: 18em;

                .entry {
                    &:hover {
                        cursor: pointer;
                        background-color: @light-green;
                    }
                    padding: 8px 12px;
                }
            }

            .map {
                border-radius: 5px;
                overflow: hidden;
            }

            .autocomplete,
            .map {
                display: none;
            }

            &.showSelectedMap {
                display: block;

                .map {
                    display: block
                }
            }
            &.showAutocomplete {
                display: block;

                .autocomplete {
                    display: block;
                }
                .map {
                    display: none;
                }
            }
        }
    }
</style>
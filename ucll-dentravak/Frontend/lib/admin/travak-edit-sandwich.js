import DenTravakAbstractElement from '../travak-abstract-element.js';
import '../common/travak-input-text.js';

class DenTravakEditSandwich extends DenTravakAbstractElement {

    constructor() {
        super('travak-admin-app')
    }

    connectedCallback() {
        super.connectedCallback();
        this.initEventListeners();
    }

    initEventListeners() {
        this.byId('save-btn').addEventListener('click', e => this.saveSandwich());
        this.byId('cancel-btn').addEventListener('click', e => this.app().showSandwichList());
    }

    saveSandwich() 
    {
        this.sandwich.name = this.byId('name').value;
        this.sandwich.ingredients = this.byId('ingredients').value;
        this.sandwich.price = this.byId('price').value;

        var addr = 'http://localhost:8080/den-travak/sandwiches';
        var restMethod = 'POST';

        if (this.sandwich.id) {
            restMethod = 'PUT';
            addr += ('/' + this.sandwich.id);
        }
        console.log(restMethod);

        (async () => 
        {
            const jsonResponse = await fetch(addr, 
            {
                method: restMethod,

                headers: 
                {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                },

                body: JSON.stringify(this.sandwich)
            });

            const response = await jsonResponse.json();
            console.log(response);
            this.sandwich.sandwichId = response.id;

            this.app().dispatchEvent(new CustomEvent('save-succeeded', { detail: response }));
            
        })();
    }

    init(sandwich) {
        if(sandwich) {
            this.sandwich = sandwich;
            this.byId('title').innerHTML = `Bewerk broodje ${sandwich.name}`;
            this.byId('name').value = sandwich.name;
            this.byId('ingredients').value = sandwich.ingredients;
            this.byId('price').value = sandwich.price;
        } else {
            this.sandwich = {};
            this.byId('title').innerHTML = `Maak een nieuw broodje`;
            this.byId('name').value = "";
            this.byId('ingredients').value = "";
            this.byId('price').value = "";
        }
    }

    get template() {
        return `
            <h4 id="title">Bewerk een broodje</h4>
            <travak-input-text id="name" label="Sandwich naam" placeholder="De naam van het broodje"></travak-input-text>
            <travak-input-text id="ingredients" label="Ingredienten" placeholder="Welke ingredienten heeft het broodje"></travak-input-text>
            <travak-input-text id="price" label="Prijs" placeholder="De prijs van het broodje"></travak-input-text>
            <button id="cancel-btn" type="button" class="btn">Cancel</button>
            <button id="save-btn" type="button" class="btn btn-primary">Save</button>
        `;
    }

}

customElements.define('travak-edit-sandwich', DenTravakEditSandwich);
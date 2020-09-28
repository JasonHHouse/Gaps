let appHasStarted;

export const CYPRESS_VALUES = {
    notBeChecked: 'not.be.checked',
    beChecked: 'be.checked',
    notBeVisible: 'not.be.visible',
    beVisible: 'be.visible'
};

export function spyOnAddEventListener(win) {
    // win = window object in our application
    const addListener = win.EventTarget.prototype.addEventListener;
    win.EventTarget.prototype.addEventListener = function (name) {
        if (name === 'change') {
            // web app added an event listener to the input box -
            // that means the web application has started
            appHasStarted = true;
            // restore the original event listener
            win.EventTarget.prototype.addEventListener = addListener;
        }
        return addListener.apply(this, arguments);
    };
}

export function searchPlexForMoviesFromSaw(cy) {
    cy.get('#dropdownMenuLink')
        .click();

    cy.get('[data-key="2"]')
        .click();

    cy.get('.card-body > .btn')
        .click();

    cy.get('label > input')
        .clear()
        .type('Saw');

    cy.get('#movies_info')
        .should('have.text', 'Showing 1 to 1 of 1 entries');

    cy.get('.card-img')
        .should('be.visible')
        .and(($img) => {
            // "naturalWidth" and "naturalHeight" are set when the image loads
            expect($img[0].naturalWidth).to.be.greaterThan(0)
        });
}

export function searchPlexForMoviesFromBestMovies(cy) {
    cy.get('#dropdownMenuLink')
        .click();

    cy.get('[data-key="5"]')
        .click();

    cy.get('.card-body > .btn')
        .click();

    //Wait for timeout from clearing data
    cy.wait(5000);
}

export function searchPlexForMoviesFromMovies(cy) {
    cy.get('#dropdownMenuLink')
        .click();

    cy.get('[data-key="1"]')
        .click();

    cy.get('.card-body > .btn')
        .click();

    cy.get('label > input')
        .clear()
        .type('Gods');

    cy.get('#movies_info')
        .should('have.text', 'Showing 1 to 1 of 1 entries (filtered from 21 total entries)');
}

export function nuke() {
    cy.request('PUT', '/nuke')
        .then((response) => {
            expect(response.body).to.have.property('code', 30);
            expect(response.body).to.have.property('reason', 'Nuke successful. All files deleted.');
        });

    //Wait for timeout from clearing data
    cy.wait(1000);
}

export function redLibraryBefore() {
    nuke();

    cy.visit('/configuration', {onBeforeLoad: spyOnAddEventListener});

    cy.get('#movieDbApiKey')
        .clear()
        .type('723b4c763114904392ca441909aa0375')
        .should('have.value', '723b4c763114904392ca441909aa0375');

    cy.get('#saveTmdbKey')
        .click();

    cy.get('#plexTab')
        .click();

    cy.get('#address')
        .clear()
        .type(atob('MTkyLjE2OC4xLjk='))
        .should('have.value', atob('MTkyLjE2OC4xLjk='));

    cy.get('#port')
        .clear()
        .type(atob('MzI0MDA='))
        .should('have.value', atob('MzI0MDA='));

    cy.get('#plexToken')
        .clear()
        .type(atob('bVF3NHVhd3hUeVlFbXFOVXJ2Qno='))
        .should('have.value', atob('bVF3NHVhd3hUeVlFbXFOVXJ2Qno='));

    cy.get('#addPlexServer')
        .click();

    //Wait for timeout from plex
    cy.wait(10000);

    cy.get('#plexSpinner')
        .should('not.be.visible');

    cy.get('#plexTestError')
        .should('not.be.visible');

    cy.get('#plexTestSuccess')
        .should('not.be.visible');

    cy.get('#plexSaveError')
        .should('not.be.visible');

    cy.get('#plexSaveSuccess')
        .should('be.visible');

    cy.get('#plexDeleteError')
        .should('not.be.visible');

    cy.get('#plexDeleteSuccess')
        .should('not.be.visible');

    cy.get('#plexDuplicateError')
        .should('not.be.visible');

    //Define card here
    cy.get('.card-header')
        .should('have.text', 'Red');

    cy.get('.list-group > :nth-child(1)')
        .should('have.text', 'Best Movies');

    cy.get('.list-group > :nth-child(2)')
        .should('have.text', 'Movies with new Metadata');

    cy.get('.list-group > :nth-child(3)')
        .should('have.text', 'Saw');
}

export function jokerLibraryBefore() {
    cy.visit('/configuration', {onBeforeLoad: spyOnAddEventListener});

    cy.get('#movieDbApiKey')
        .clear()
        .type('723b4c763114904392ca441909aa0375')
        .should('have.value', '723b4c763114904392ca441909aa0375');

    cy.get('#saveTmdbKey')
        .click();

    cy.get('#plexTab')
        .click();

    cy.get('#address')
        .clear()
        .type('192.168.1.8')
        .should('have.value', '192.168.1.8');

    cy.get('#port')
        .clear()
        .type(atob('MzI0MDA='))
        .should('have.value', atob('MzI0MDA='));

    cy.get('#plexToken')
        .clear()
        .type(atob('bVF3NHVhd3hUeVlFbXFOVXJ2Qno='))
        .should('have.value', atob('bVF3NHVhd3hUeVlFbXFOVXJ2Qno='));

    cy.get('#addPlexServer')
        .click();

    cy.get('#plexSpinner')
        .should('not.be.visible');

    cy.get('#plexTestError')
        .should('not.be.visible');

    cy.get('#plexTestSuccess')
        .should('not.be.visible');

    cy.get('#plexSaveError')
        .should('not.be.visible');

    cy.get('#plexSaveSuccess')
        .should('be.visible');

    cy.get('#plexDeleteError')
        .should('not.be.visible');

    cy.get('#plexDeleteSuccess')
        .should('not.be.visible');

    cy.get('#plexDuplicateError')
        .should('not.be.visible');
}
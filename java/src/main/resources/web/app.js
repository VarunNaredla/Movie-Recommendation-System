const select = document.querySelector('#movie-select');
const form = document.querySelector('#recommend-form');
const button = document.querySelector('#submit-button');
const feedback = document.querySelector('#feedback');
const resultGrid = document.querySelector('#movie-grid');
const catalogGrid = document.querySelector('#catalog-grid');
const resultsHead = document.querySelector('#results-head');
const searchInput = document.querySelector('#catalog-search');
const genreFilter = document.querySelector('#genre-filter');
const favoritesButton = document.querySelector('#favorites-filter');
const wikiTitles = {
  'Inception':'Inception (2010 film)', 'Interstellar':'Interstellar (film)',
  'They Call Him OG':'They Call Him OG', 'Titanic':'Titanic (1997 film)',
  'The Notebook':'The Notebook', 'Joker':'Joker (2019 film)',
  'Parasite':'Parasite (2019 film)', 'The Dark Knight':'The Dark Knight'
};
const wikiCache = new Map();
let movies = [];
let showFavorites = false;
let saved = new Set();
try { saved = new Set(JSON.parse(localStorage.getItem('movie-match-favorites') || '[]')); } catch { saved = new Set(); }

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
}
function card(movie, index) {
  return `<article class="movie-card"><div class="movie-top"><span class="rank">${String(index + 1).padStart(2,'0')} &nbsp; MATCH</span><span class="rating">★ ${Number(movie.rating).toFixed(1)}</span></div><h4>${escapeHtml(movie.title)}</h4><div class="genres">${movie.genres.map(g=>`<span class="genre">${escapeHtml(g)}</span>`).join('')}</div><button class="card-info" type="button" data-detail="${escapeHtml(movie.title)}">Film details ↗</button></article>`;
}
function renderCatalog() {
  const query = searchInput.value.trim().toLowerCase();
  const genre = genreFilter.value;
  const visible = movies.filter(movie => {
    const matchesQuery = `${movie.title} ${movie.genres.join(' ')}`.toLowerCase().includes(query);
    return matchesQuery && (!genre || movie.genres.includes(genre)) && (!showFavorites || saved.has(movie.title));
  });
  catalogGrid.innerHTML = visible.map(movie => `<article class="catalog-item"><div class="catalog-item-top"><div><strong>${escapeHtml(movie.title)}</strong><span class="catalog-rating">★ ${Number(movie.rating).toFixed(1)}</span></div><button class="favorite-toggle ${saved.has(movie.title) ? 'is-saved' : ''}" type="button" data-favorite="${escapeHtml(movie.title)}" aria-label="${saved.has(movie.title) ? 'Remove' : 'Save'} ${escapeHtml(movie.title)} ${saved.has(movie.title) ? 'from' : 'to'} saved films" aria-pressed="${saved.has(movie.title)}">${saved.has(movie.title) ? '♥' : '♡'}</button></div><div class="catalog-genres">${movie.genres.map(g=>`<span>${escapeHtml(g)}</span>`).join('')}</div><div class="catalog-actions"><button type="button" data-detail="${escapeHtml(movie.title)}">About this film ↗</button><button type="button" data-pick="${escapeHtml(movie.title)}">Use for recommendations →</button></div><div class="wiki-detail" id="wiki-${encodeURIComponent(movie.title)}" hidden></div></article>`).join('');
  document.querySelector('#empty-state').hidden = visible.length > 0;
  document.querySelector('#catalog-total').textContent = showFavorites ? `${visible.length} SAVED` : `${visible.length} OF ${movies.length} FILMS`;
}
async function getWikiSummary(title) {
  if (wikiCache.has(title)) return wikiCache.get(title);
  const page = wikiTitles[title] || title;
  const promise = fetch(`https://en.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(page.replaceAll(' ', '_'))}`, {headers:{'Accept':'application/json'}})
    .then(async response => { if (!response.ok) throw new Error('Wikipedia could not find a summary for this film.'); return response.json(); });
  wikiCache.set(title, promise);
  try { return await promise; } catch (error) { wikiCache.delete(title); throw error; }
}
async function openDetails(title, panel) {
  panel.hidden = false;
  if (panel.dataset.loaded === 'true') return;
  panel.innerHTML = '<p class="wiki-loading">Looking up the film on Wikipedia…</p>';
  try {
    const info = await getWikiSummary(title);
    if (!info.extract) throw new Error('A summary is not available for this page.');
    const image = info.thumbnail?.source ? `<a class="wiki-image-link" href="${escapeHtml(info.content_urls?.desktop?.page || info.content_urls?.mobile?.page || '#')}" target="_blank" rel="noopener noreferrer"><img src="${escapeHtml(info.thumbnail.source)}" alt="${escapeHtml(info.title || title)} on Wikipedia" loading="lazy"></a>` : '';
    const source = info.content_urls?.desktop?.page || `https://en.wikipedia.org/wiki/${encodeURIComponent(wikiTitles[title] || title).replaceAll('%20','_')}`;
    panel.innerHTML = `${image}<div class="wiki-copy"><div class="wiki-kicker">FROM WIKIPEDIA</div><p>${escapeHtml(info.extract)}</p><a class="wiki-source" href="${escapeHtml(source)}" target="_blank" rel="noopener noreferrer">Read the Wikipedia article ↗</a><button type="button" data-wiki-close="${escapeHtml(title)}">Close details</button></div>`;
    panel.dataset.loaded = 'true';
  } catch (error) {
    const page = `https://en.wikipedia.org/wiki/${encodeURIComponent(wikiTitles[title] || title).replaceAll('%20','_')}`;
    panel.innerHTML = `<div class="wiki-copy"><div class="wiki-kicker">WIKIPEDIA ARTICLE</div><p>${escapeHtml(error.message)} You can still read the source article directly.</p><a class="wiki-source" href="${page}" target="_blank" rel="noopener noreferrer">Open ${escapeHtml(title)} on Wikipedia ↗</a><button type="button" data-wiki-retry="${escapeHtml(title)}">Try loading again</button></div>`;
  }
}
async function loadCatalog() {
  try {
    const response = await fetch('/api/movies');
    if (!response.ok) throw new Error('Could not load the movie collection. Refresh to try again.');
    movies = await response.json();
    for (const movie of movies) {
      const option = document.createElement('option'); option.value = movie.title; option.textContent = movie.title; select.append(option);
    }
    [...new Set(movies.flatMap(movie=>movie.genres))].sort().forEach(genre=>{const option=document.createElement('option');option.value=genre;option.textContent=genre;genreFilter.append(option);});
    document.querySelector('#catalog-count').textContent = `${movies.length} stories in our collection`;
    renderCatalog();
  } catch (error) { feedback.textContent = error.message; }
}
form.addEventListener('submit', async event => {
  event.preventDefault();
  if (!select.value) { select.focus(); return; }
  feedback.textContent = '';
  button.disabled = true; button.innerHTML = 'Finding your next film…';
  try {
    const response = await fetch(`/api/recommend?title=${encodeURIComponent(select.value)}&limit=6`);
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Recommendations could not be loaded.');
    document.querySelector('#results-title').textContent = `Because you loved ${data.basedOn}`;
    resultGrid.innerHTML = data.recommendations.map(card).join('');
    resultsHead.hidden = false;
    if (!data.recommendations.length) feedback.textContent = 'No recommendations are available yet.';
    resultsHead.scrollIntoView({behavior:'smooth',block:'start'});
  } catch (error) { feedback.textContent = error.message; }
  finally { button.disabled = false; button.innerHTML = 'Find my next film <span>→</span>'; }
});
document.querySelector('#reset-button').addEventListener('click',()=>{resultsHead.hidden=true;resultGrid.innerHTML='';feedback.textContent='';select.value='';document.querySelector('#discover').scrollIntoView({behavior:'smooth'});});
searchInput.addEventListener('input',renderCatalog);
genreFilter.addEventListener('change',renderCatalog);
favoritesButton.addEventListener('click',()=>{showFavorites=!showFavorites;favoritesButton.classList.toggle('active',showFavorites);favoritesButton.setAttribute('aria-pressed',String(showFavorites));renderCatalog();});
document.addEventListener('click',event=>{
  const favorite=event.target.closest('[data-favorite]');
  if(favorite){const title=favorite.dataset.favorite;saved.has(title)?saved.delete(title):saved.add(title);try{localStorage.setItem('movie-match-favorites',JSON.stringify([...saved]));}catch{}renderCatalog();return;}
  const pick=event.target.closest('[data-pick]');
  if(pick){select.value=pick.dataset.pick;document.querySelector('#discover').scrollIntoView({behavior:'smooth'});select.focus();return;}
  const close=event.target.closest('[data-wiki-close]');
  if(close){const panel=close.closest('.wiki-detail');panel.hidden=true;return;}
  const retry=event.target.closest('[data-wiki-retry]');
  if(retry){openDetails(retry.dataset.wikiRetry,retry.closest('.wiki-detail'));return;}
  const detail=event.target.closest('[data-detail]');
  if(detail){const title=detail.dataset.detail;const panel=detail.closest('.catalog-item')?.querySelector('.wiki-detail') || detail.closest('.movie-card')?.querySelector('.wiki-detail');if(panel){panel.hidden=!panel.hidden;if(!panel.hidden)openDetails(title,panel);else panel.dataset.loaded='false';}else openCatalogDetails(title,detail);}
});
function openCatalogDetails(title,buttonElement) {
  const article=buttonElement.closest('.movie-card');
  let panel=article.querySelector('.wiki-detail');
  if(!panel){panel=document.createElement('div');panel.className='wiki-detail';article.append(panel);}
  panel.hidden=!panel.hidden;
  if(!panel.hidden)openDetails(title,panel);
}
loadCatalog();

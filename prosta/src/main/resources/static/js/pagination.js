document.addEventListener('DOMContentLoaded', function () {
    // この方式は取得済みの行を非表示にするだけ。解答フォームの値はDOMに残してページ間で保持する。
    document.querySelectorAll('[data-pagination-target]').forEach(function (container) {
        const target = document.getElementById(container.dataset.paginationTarget);
        if (!target) return;
        const rows = target.querySelectorAll(container.dataset.paginationRows);
        const pageSize = Number(container.dataset.paginationSize);
        const pageCount = Math.ceil(rows.length / pageSize);
        if (pageCount <= 1) return;

        const previous = document.createElement('button');
        previous.id = 'p-prev';
        previous.type = 'button';
        previous.className = 'btn-pagination';
        previous.textContent = '前へ';
        const next = document.createElement('button');
        next.id = 'p-next';
        next.type = 'button';
        next.className = 'btn-pagination';
        next.textContent = '次へ';
        const label = document.createElement('label');
        label.className = 'page-selector';
        label.append('ページ ');
        const select = document.createElement('select');
        select.setAttribute('aria-label', 'ページを選択');
        for (let page = 1; page <= pageCount; page++) {
            select.add(new Option(String(page), String(page)));
        }
        label.append(select, ' / ' + pageCount);

        let currentPage = 1;
        function showPage(page) {
            currentPage = page;
            rows.forEach(function (row, index) {
                row.style.display = index >= (page - 1) * pageSize && index < page * pageSize ? '' : 'none';
            });
            previous.disabled = page === 1;
            next.disabled = page === pageCount;
            select.value = String(page);
        }
        previous.addEventListener('click', function () { showPage(currentPage - 1); });
        next.addEventListener('click', function () { showPage(currentPage + 1); });
        select.addEventListener('change', function () { showPage(Number(select.value)); });
        container.append(previous, label, next);
        showPage(1);
    });

    // DB側でページ分割する一覧はこちら。各optionにテンプレートが設定したURLへ移動する。
    document.querySelectorAll('select[data-page-navigation]').forEach(function (select) {
        select.addEventListener('change', function () {
            location.assign(select.selectedOptions[0].dataset.pageUrl);
        });
    });
});

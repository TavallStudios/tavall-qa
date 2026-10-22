(() => {
    'use strict';

    const INITIAL_VISIBLE_TASKS = 3;
    const EXPAND_BATCH_SIZE = 4;

    function updateExpander(group) {
        const items = Array.from(group.querySelectorAll('[data-task-item]'));
        const button = group.querySelector('[data-task-expander]');
        if (!button) {
            return;
        }

        const hiddenItems = items.filter(item => item.hidden);
        if (hiddenItems.length === 0) {
            button.hidden = true;
            button.setAttribute('aria-expanded', 'true');
            return;
        }

        const revealCount = Math.min(EXPAND_BATCH_SIZE, hiddenItems.length);
        button.hidden = false;
        button.setAttribute('aria-expanded', 'false');
        button.textContent = hiddenItems.length <= EXPAND_BATCH_SIZE
                ? `Show remaining ${hiddenItems.length}`
                : `Show ${revealCount} more · ${hiddenItems.length} remaining`;
    }

    function initializeTaskGroup(group) {
        const items = Array.from(group.querySelectorAll('[data-task-item]'));
        items.forEach((item, index) => {
            item.hidden = index >= INITIAL_VISIBLE_TASKS;
        });

        const button = group.querySelector('[data-task-expander]');
        if (button) {
            button.addEventListener('click', () => {
                const hiddenItems = items.filter(item => item.hidden);
                hiddenItems.slice(0, EXPAND_BATCH_SIZE).forEach(item => {
                    item.hidden = false;
                });
                updateExpander(group);
            });
        }
        updateExpander(group);
    }

    const SELECT_ACRONYMS = new Set(['QA', 'NHV', 'HV', 'UUID', 'FFA', 'API', 'URL', 'UI', 'PR', 'ID']);

    function humanizeOptionLabel(rawLabel) {
        const raw = (rawLabel || '').trim();
        if (!raw) return '';

        const parts = raw.split(' · ');
        const candidate = parts[0];
        if (/^[A-Z0-9_ ]+$/.test(candidate)) {
            parts[0] = candidate.replaceAll('_', ' ').split(/\s+/).map(token => {
                if (!token) return token;
                if (SELECT_ACRONYMS.has(token)) return token;
                return `${token.charAt(0)}${token.slice(1).toLowerCase()}`;
            }).join(' ');
        } else {
            parts[0] = candidate.replaceAll('_', ' ');
        }
        return parts.join(' · ');
    }

    function initializeSoftSelect(select, index) {
        if (select.dataset.softSelectEnhanced === 'true') return;
        select.dataset.softSelectEnhanced = 'true';
        select.classList.add('public-soft-select-native');
        select.setAttribute('aria-hidden', 'true');
        select.tabIndex = -1;

        const wrapper = document.createElement('div');
        wrapper.className = 'public-soft-select';
        const trigger = document.createElement('button');
        trigger.type = 'button';
        trigger.className = 'public-soft-select-trigger';
        trigger.setAttribute('aria-haspopup', 'listbox');
        trigger.setAttribute('aria-expanded', 'false');
        trigger.setAttribute('aria-label', select.closest('label')?.querySelector(':scope > span')?.textContent?.trim() || 'Choose option');

        const value = document.createElement('span');
        value.className = 'public-soft-select-value';
        trigger.appendChild(value);

        const menu = document.createElement('div');
        menu.className = 'public-soft-select-menu';
        menu.id = `task-board-select-${index}`;
        menu.setAttribute('role', 'listbox');
        trigger.setAttribute('aria-controls', menu.id);
        wrapper.append(trigger, menu);
        select.insertAdjacentElement('afterend', wrapper);

        const optionButtons = [];
        Array.from(select.options).forEach((option, optionIndex) => {
            const item = document.createElement('button');
            item.type = 'button';
            item.className = 'public-soft-select-option';
            item.dataset.optionIndex = String(optionIndex);
            item.setAttribute('role', 'option');
            item.textContent = humanizeOptionLabel(option.textContent);
            item.disabled = option.disabled;
            menu.appendChild(item);
            optionButtons.push(item);
        });

        const sync = () => {
            const selectedIndex = Math.max(select.selectedIndex, 0);
            const option = select.options[selectedIndex];
            value.textContent = option ? humanizeOptionLabel(option.textContent) : 'Choose option';
            optionButtons.forEach((item, itemIndex) => {
                const selected = itemIndex === selectedIndex;
                item.classList.toggle('is-selected', selected);
                item.setAttribute('aria-selected', String(selected));
            });
        };

        const close = (restoreFocus = false) => {
            wrapper.classList.remove('is-open', 'is-upward');
            trigger.setAttribute('aria-expanded', 'false');
            optionButtons.forEach(item => item.classList.remove('is-active'));
            if (restoreFocus) trigger.focus();
        };

        const focusOption = requestedIndex => {
            const enabled = optionButtons.filter(item => !item.disabled);
            if (enabled.length === 0) return;
            const requested = optionButtons[requestedIndex];
            const target = requested && !requested.disabled ? requested : enabled[0];
            optionButtons.forEach(item => item.classList.toggle('is-active', item === target));
            target.focus({ preventScroll: true });
            target.scrollIntoView({ block: 'nearest' });
        };

        const open = () => {
            wrapper.classList.add('is-open');
            trigger.setAttribute('aria-expanded', 'true');
            requestAnimationFrame(() => {
                const rect = wrapper.getBoundingClientRect();
                const menuHeight = Math.min(menu.scrollHeight, Math.max(180, window.innerHeight * .42));
                const spaceBelow = window.innerHeight - rect.bottom;
                const spaceAbove = rect.top;
                wrapper.classList.toggle('is-upward', spaceBelow < menuHeight + 18 && spaceAbove > spaceBelow);
            });
        };

        const choose = optionIndex => {
            const option = select.options[optionIndex];
            if (!option || option.disabled) return;
            const changed = select.selectedIndex !== optionIndex;
            select.selectedIndex = optionIndex;
            sync();
            close(true);
            if (changed) {
                select.dispatchEvent(new Event('input', { bubbles: true }));
                select.dispatchEvent(new Event('change', { bubbles: true }));
            }
        };

        trigger.addEventListener('click', event => {
            event.preventDefault();
            if (wrapper.classList.contains('is-open')) close();
            else open();
        });
        trigger.addEventListener('keydown', event => {
            if (event.key === 'Escape' && wrapper.classList.contains('is-open')) {
                event.preventDefault();
                close(true);
                return;
            }
            if (!['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(event.key)) return;
            event.preventDefault();
            open();
            const selected = Math.max(select.selectedIndex, 0);
            const target = event.key === 'ArrowUp' ? Math.max(0, selected - 1) : selected;
            focusOption(target);
        });

        optionButtons.forEach((item, optionIndex) => {
            item.addEventListener('click', event => {
                event.preventDefault();
                choose(optionIndex);
            });
            item.addEventListener('pointerenter', () => {
                optionButtons.forEach(other => other.classList.remove('is-active'));
            });
            item.addEventListener('keydown', event => {
                const current = optionButtons.indexOf(item);
                if (event.key === 'Escape') {
                    event.preventDefault();
                    close(true);
                    return;
                }
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    choose(current);
                    return;
                }
                if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
                event.preventDefault();
                let next = current;
                if (event.key === 'Home') next = 0;
                else if (event.key === 'End') next = optionButtons.length - 1;
                else next += event.key === 'ArrowDown' ? 1 : -1;
                next = Math.max(0, Math.min(optionButtons.length - 1, next));
                while (optionButtons[next]?.disabled && next !== current) {
                    next += event.key === 'ArrowDown' ? 1 : -1;
                    if (next < 0 || next >= optionButtons.length) break;
                }
                focusOption(Math.max(0, Math.min(optionButtons.length - 1, next)));
            });
        });

        select.addEventListener('change', sync);
        wrapper.addEventListener('focusout', () => {
            setTimeout(() => {
                if (!wrapper.contains(document.activeElement)) close();
            }, 0);
        });
        document.addEventListener('pointerdown', event => {
            if (!wrapper.contains(event.target)) close();
        });
        sync();
    }

    function initializeStaffControls() {
        if (!document.body.dataset.staffWorkspaceVariant) return;
        document.querySelectorAll('.public-staff-form select, .public-staff-state-picker select')
                .forEach((select, index) => initializeSoftSelect(select, index));
    }

    function initializeStaffWorkspace() {
        const body = document.body;
        if (body.dataset.staffWorkspaceVariant !== 'C') {
            return;
        }

        const buttons = Array.from(document.querySelectorAll('[data-workspace-focus]'));
        const panels = Array.from(document.querySelectorAll('[data-workspace-panel]'));
        if (buttons.length === 0 || panels.length === 0) {
            return;
        }

        const normalizeFocus = (value) => ['QA', 'EDIT', 'THREAD'].includes(value) ? value : 'OVERVIEW';
        const focusFromHash = () => {
            if (location.hash === '#workspace-qa') return 'QA';
            if (location.hash === '#task-edit' || location.hash === '#task-add-destination') return 'EDIT';
            if (location.hash === '#task-thread') return 'THREAD';
            return 'OVERVIEW';
        };

        const applyFocus = (requestedFocus, updateHash) => {
            const focus = normalizeFocus(requestedFocus);
            buttons.forEach(button => {
                const active = button.dataset.workspaceFocus === focus;
                button.classList.toggle('active', active);
                button.setAttribute('aria-pressed', String(active));
            });
            panels.forEach(panel => {
                panel.hidden = focus !== 'OVERVIEW' && panel.dataset.workspacePanel !== focus;
            });
            if (updateHash) {
                const hash = focus === 'EDIT' ? '#task-edit' : focus === 'THREAD' ? '#task-thread' : focus === 'QA' ? '#workspace-qa' : '';
                history.replaceState(null, '', `${location.pathname}${location.search}${hash}`);
            }
            if (focus !== 'OVERVIEW') {
                panels.find(panel => !panel.hidden)?.scrollIntoView({ block: 'start', behavior: 'instant' });
            }
        };

        buttons.forEach(button => {
            button.addEventListener('click', () => applyFocus(button.dataset.workspaceFocus || 'OVERVIEW', true));
        });
        applyFocus(focusFromHash(), false);
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-task-group]').forEach(initializeTaskGroup);
        initializeStaffControls();
        initializeStaffWorkspace();
    });
})();

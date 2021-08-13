import React from 'react';
import {Link, withRouter} from 'react-router-dom';

import styles from './Main.module.css';

function Navbar() {
    return (
        <div className={styles.Navbar}>
            <Link to="/task1">Task 1</Link>
            <Link to="/task2">Task 2</Link>
        </div>
    );
}

export default withRouter(Navbar);
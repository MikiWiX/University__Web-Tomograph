import React from 'react';
import { Switch, Route, Redirect } from 'react-router-dom';

import Task1 from './pages/Task1';
import Task2 from './pages/Task2';

const Main = () => {
  return (
    <Switch> {/* The Switch decides which component to show based on the current URL.*/}
      <Route exact path='/' render={() => <Redirect to='/task1' />} />
      <Route exact path='/task1' component={Task1} />
      <Route exact path='/task2' component={Task2} />
    </Switch>
  );
}

export default Main;